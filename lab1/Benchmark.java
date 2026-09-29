package lab1;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Секундомер этапа 0: один забег = T потоков, каждый крутит record() ровно seconds секунд.
 * Потоки стартуют по защёлке, каждый считает свои вызовы в локальной переменной,
 * флаг остановки общий. Возвращает суммарную пропускную способность в оп/сек.
 */
public final class Benchmark {

    static final int WARMUP_SECONDS = 5;
    static final int MEASURE_SECONDS = 5;
    static final int RUNS = 5;

    private Benchmark() {
    }

    static double run(MetricsCollector collector, long[] values, int threads, int seconds) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        long[] ops = new long[threads];
        Thread[] workers = new Thread[threads];

        for (int k = 0; k < threads; k++) {
            int id = k;
            workers[k] = new Thread(() -> {
                long localCount = 0;
                int i = (int) ((long) id * 1000 % values.length);
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (!stop.get()) {
                    collector.record(values[i]);
                    localCount++;
                    i++;
                    if (i == values.length) {
                        i = 0;
                    }
                }
                ops[id] = localCount;
            }, "worker-" + k);
            workers[k].start();
        }

        long t0 = System.nanoTime();
        start.countDown();
        Thread.sleep(seconds * 1000L);
        stop.set(true);
        long t1 = System.nanoTime();

        for (Thread worker : workers) {
            worker.join();
        }

        long total = 0;
        for (long op : ops) {
            total += op;
        }
        return total / ((t1 - t0) / 1e9);
    }

    static double measurePoint(MetricsCollector collector, long[] values, int threads, int seconds)
            throws InterruptedException {
        run(collector, values, threads, WARMUP_SECONDS); // прогрев, результат выбрасываем

        double[] results = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            results[i] = run(collector, values, threads, seconds);
            System.out.printf("  run %d/%d: %.2f M ops/s%n", i + 1, RUNS, results[i] / 1e6);
        }

        Arrays.sort(results);
        double median = results[RUNS / 2];

        Snapshot snapshot = collector.snapshot();
        System.out.printf("  snapshot: count=%d sum=%d min=%d max=%d p50=%d p99=%d%n",
                snapshot.count(), snapshot.sum(), snapshot.min(), snapshot.max(),
                snapshot.p50(), snapshot.p99());

        return median;
    }

    static MetricsCollector createCollector(String name) {
        return switch (name) {
            case "one-thread" -> new OneThreadMetricCollector();
            default -> throw new IllegalArgumentException("Unknown collector: " + name);
        };
    }

    public static void main(String[] args) throws InterruptedException {
        String name = args.length > 0 ? args[0] : "one-thread";
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : MEASURE_SECONDS;

        long[] values = ZipfGenerator.generate();
        MetricsCollector collector = createCollector(name);

        System.out.printf("collector=%s threads=%d seconds=%d warmup=%d runs=%d%n",
                name, threads, seconds, WARMUP_SECONDS, RUNS);

        double median = measurePoint(collector, values, threads, seconds);
        System.out.printf("RESULT %s T=%d: %.2f M ops/s%n", name, threads, median / 1e6);
    }
}
