package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков. */
public final class Scheduler {
    /** Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner. */
    boolean logging;
    /** Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100". */
    final List<String> log = new ArrayList<>();
    /** null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2). */
    String deadlock;

    private static final ThreadLocal<Scheduler> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Worker> ME = new ThreadLocal<>();

    private final List<Worker> workers = new ArrayList<>();
    private final List<SchedLock> locks = new ArrayList<>();
    private final Semaphore handoff = new Semaphore(0);
    private final StringBuilder trace = new StringBuilder();

    public Scheduler() { }

    static Scheduler current() {
        Scheduler s = CURRENT.get();
        if (s == null) throw new IllegalStateException("no scheduler on this thread");
        return s;
    }

    /** Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn. */
    public void spawn(Runnable body) {
        workers.add(new Worker(workers.size(), body));
    }

    void registerLock(SchedLock lock) {
        locks.add(lock);
    }

    /** Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last). */
    public void run(Strategy strategy) {
        deadlock = null;
        trace.setLength(0);
        log.clear();

        for (Worker w : workers) {
            w.thread = new Thread(() -> {
                CURRENT.set(this);
                ME.set(w);
                w.wake.acquireUninterruptibly();
                try {
                    w.body.run();
                } catch (RunAborted ignored) {
                    // abort после deadlock
                } finally {
                    w.finished = true;
                    ME.remove();
                    CURRENT.remove();
                    handoff.release();
                }
            }, "sched-" + (char) ('A' + w.id));
            w.thread.start();
        }

        // пауза перед первой операцией без трассы
        for (Worker w : workers) {
            w.wake.release();
            handoff.acquireUninterruptibly();
        }

        int last = -1;
        while (true) {
            refreshAwaits();
            refreshTryingLocks();
            List<Integer> enabled = collectEnabled();
            if (enabled.isEmpty()) {
                if (allFinished()) break;
                deadlock = formatHeld();
                abortAll();
                break;
            }

            int pick = strategy.pick(enabled, last);
            trace.append((char) ('A' + pick));
            last = pick;

            workers.get(pick).wake.release();
            handoff.acquireUninterruptibly();
        }

        for (Worker w : workers) {
            try {
                w.thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB". */
    public String traceString() {
        return trace.toString();
    }

    /** window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна. */
    public static void point() {
        current().pointImpl();
    }

    /** Ждать, пока cond не станет true. До этого поток не попадает в enabled. */
    public static void await(BooleanSupplier cond) {
        current().awaitImpl(cond);
    }

    // вызовы из Shared / SchedLock

    Worker me() {
        Worker w = ME.get();
        if (w == null) throw new IllegalStateException("no worker on this thread");
        return w;
    }

    void beforeShared() {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (w.touched) pause(w);
    }

    void afterRead(String name, Object value) {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (logging) log.add(letter(w) + "  " + name + " -> " + value);
        w.touched = true;
    }

    void afterWrite(String name, Object value) {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (logging) log.add(letter(w) + "  " + name + " = " + value);
        w.touched = true;
    }

    void afterLockOp(String op, String name) {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (logging) log.add(letter(w) + "  " + op + " " + name);
        w.touched = true;
    }

    void pause(Worker w) {
        handoff.release();
        w.wake.acquireUninterruptibly();
        w.touched = false;
        if (w.aborted) throw new RunAborted();
    }

    void clearLockWaiters(SchedLock lock) {
        for (Worker w : workers) {
            if (w.waitingLock == lock) w.waitingLock = null;
        }
    }

    private void pointImpl() {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (logging) log.add(letter(w) + "  window");
        w.touched = true;
    }

    private void awaitImpl(BooleanSupplier cond) {
        Worker w = me();
        if (w.aborted) throw new RunAborted();
        if (w.touched || !cond.getAsBoolean()) {
            while (!cond.getAsBoolean()) {
                w.waitingCond = cond;
                pause(w);
                w.waitingCond = null;
            }
            if (w.touched) pause(w);
        }
        if (logging) log.add(letter(w) + "  await done");
        w.touched = true;
    }

    // если поток стоит на паузе перед lock, а лок уже занят — он waiter, не enabled
    private void refreshTryingLocks() {
        for (Worker w : workers) {
            if (w.finished || w.aborted) continue;
            SchedLock L = w.tryingLock;
            if (L != null && L.owner != null && L.owner != w) {
                w.waitingLock = L;
                w.tryingLock = null;
            }
        }
    }

    private void refreshAwaits() {
        for (Worker w : workers) {
            if (w.waitingCond != null && w.waitingCond.getAsBoolean()) {
                w.waitingCond = null;
            }
        }
    }

    private List<Integer> collectEnabled() {
        List<Integer> enabled = new ArrayList<>();
        for (Worker w : workers) {
            if (w.finished || w.aborted) continue;
            if (w.waitingCond != null) continue;
            if (w.waitingLock != null) continue;
            enabled.add(w.id);
        }
        return enabled;
    }

    private boolean allFinished() {
        for (Worker w : workers) {
            if (!w.finished) return false;
        }
        return true;
    }

    private String formatHeld() {
        StringBuilder sb = new StringBuilder("held={");
        boolean first = true;
        for (SchedLock lock : locks) {
            if (lock.owner == null) continue;
            if (!first) sb.append(", ");
            first = false;
            sb.append(lock.name).append(':').append(letter(lock.owner));
        }
        sb.append('}');
        return sb.toString();
    }

    private void abortAll() {
        for (Worker w : workers) {
            if (!w.finished) {
                w.aborted = true;
                w.waitingCond = null;
                w.waitingLock = null;
                w.tryingLock = null;
                w.wake.release();
                handoff.acquireUninterruptibly();
            }
        }
    }

    private static String letter(Worker w) {
        return String.valueOf((char) ('A' + w.id));
    }

    static final class Worker {
        final int id;
        final Runnable body;
        final Semaphore wake = new Semaphore(0);
        Thread thread;
        boolean touched = true;
        volatile boolean finished;
        volatile boolean aborted;
        volatile BooleanSupplier waitingCond;
        volatile SchedLock waitingLock;
        volatile SchedLock tryingLock;

        Worker(int id, Runnable body) {
            this.id = id;
            this.body = body;
        }
    }

    static final class RunAborted extends RuntimeException {
        RunAborted() { super(null, null, false, false); }
    }
}
