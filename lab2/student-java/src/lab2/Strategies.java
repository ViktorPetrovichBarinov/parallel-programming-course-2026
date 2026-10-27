package lab2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() { }

    /** Всегда первый поток из enabled. */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            return enabled.get(0);
        }
    }

    /** Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1). */
    public static final class Replay implements Strategy {
        private final String trace;
        private int pos;

        public Replay(String trace) { this.trace = trace; }

        /** Сбросить позицию в трассе перед прогоном. */
        public void beginRun() { pos = 0; }

        public int pick(List<Integer> enabled, int last) {
            if (pos >= trace.length()) {
                throw new IllegalStateException("replay: trace ended, need more decisions");
            }
            int id = trace.charAt(pos++) - 'A';
            if (!enabled.contains(id)) {
                throw new IllegalStateException("replay: " + (char) ('A' + id) + " not enabled");
            }
            return id;
        }
    }

    /** Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed). */
    public static final class RandomPick implements Strategy {
        private final Random rnd;

        public RandomPick(long seed) { rnd = new Random(seed); }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(rnd.nextInt(enabled.size()));
        }
    }

    /** Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     *  bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE. */
    public static final class Dfs implements Strategy {
        private final int bound;
        private final List<List<Integer>> forks = new ArrayList<>();
        private final List<Integer> choice = new ArrayList<>();
        private int depth;
        private int preemptions;

        public Dfs(int bound) { this.bound = bound; }

        /** Подготовиться к новому прогону (позиция в пути, счётчик вытеснений). */
        public void beginRun() {
            depth = 0;
            preemptions = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (depth == forks.size()) {
                List<Integer> opts = new ArrayList<>();
                for (int id : enabled) {
                    boolean preempt = last != -1 && id != last && enabled.contains(last);
                    if (!preempt || preemptions < bound) opts.add(id);
                }
                forks.add(opts);
                choice.add(0);
            }
            int id = forks.get(depth).get(choice.get(depth));
            if (last != -1 && id != last && enabled.contains(last)) preemptions++;
            depth++;
            return id;
        }

        /** Выбрать следующий путь; false — дерево обойдено. */
        public boolean endRun() {
            // укоротить путь до фактической длины прогона
            while (forks.size() > depth) {
                forks.remove(forks.size() - 1);
                choice.remove(choice.size() - 1);
            }
            for (int d = choice.size() - 1; d >= 0; d--) {
                if (choice.get(d) + 1 < forks.get(d).size()) {
                    choice.set(d, choice.get(d) + 1);
                    while (forks.size() > d + 1) {
                        forks.remove(forks.size() - 1);
                        choice.remove(choice.size() - 1);
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
