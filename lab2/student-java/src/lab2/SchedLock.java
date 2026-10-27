package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;
    final Scheduler scheduler;
    Scheduler.Worker owner;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.scheduler = s;
        this.name = name;
        s.registerLock(this);
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        Scheduler.Worker w = scheduler.me();
        while (true) {
            if (w.aborted) throw new Scheduler.RunAborted();
            w.tryingLock = this;
            scheduler.beforeShared();
            w.tryingLock = null;
            if (w.aborted) throw new Scheduler.RunAborted();
            if (owner == null) {
                owner = w;
                scheduler.afterLockOp("lock", name);
                return;
            }
            w.waitingLock = this;
            scheduler.pause(w);
            w.waitingLock = null;
        }
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        Scheduler.Worker w = scheduler.me();
        if (w.aborted) return;
        scheduler.beforeShared();
        if (w.aborted) return;
        if (owner != w) throw new IllegalStateException("unlock " + name + " by non-owner");
        owner = null;
        scheduler.clearLockWaiters(this);
        scheduler.afterLockOp("unlock", name);
    }
}
