package lab1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InconsistencyTest {

    static final int WRITERS = 4;
    static final int SNAPSHOT_ATTEMPTS = 10_000;

    private InconsistencyTest() {
    }

    public static void main(String[] args) throws InterruptedException {
        String name = args.length > 0 ? args[0] : "striped";
        int writers = args.length > 1 ? Integer.parseInt(args[1]) : WRITERS;
        int attempts = args.length > 2 ? Integer.parseInt(args[2]) : SNAPSHOT_ATTEMPTS;

        long[] values = ZipfGenerator.generate();
        int mask = values.length - 1;
        MetricsCollector collector = Benchmark.createCollector(name);

        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        long[] calls = new long[writers];
        Thread[] threads = new Thread[writers];

        for (int k = 0; k < writers; k++) {
            int id = k;
            threads[k] = new Thread(() -> {
                long localCount = 0;
                int i = id * 1000 & mask;
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                while (!stop.get()) {
                    collector.record(values[i]);
                    localCount++;
                    i = i + 1 & mask;
                }
                calls[id] = localCount;
            }, "writer-" + k);
            threads[k].start();
        }

        start.countDown();

        int broken = 0;
        int lessThanCount = 0;
        int greaterThanCount = 0;
        for (int i = 0; i < attempts; i++) {
            Snapshot snapshot = collector.snapshot();
            long bucketSum = 0;
            for (long bucket : snapshot.buckets()) {
                bucketSum += bucket;
            }
            if (bucketSum != snapshot.count()) {
                broken++;
                if (bucketSum < snapshot.count()) {
                    lessThanCount++;
                } else {
                    greaterThanCount++;
                }
            }
        }

        stop.set(true);
        for (Thread thread : threads) {
            thread.join();
        }

        long totalCalls = 0;
        for (long call : calls) {
            totalCalls += call;
        }

        Snapshot finalSnapshot = collector.snapshot();
        long finalBucketSum = 0;
        for (long bucket : finalSnapshot.buckets()) {
            finalBucketSum += bucket;
        }

        System.out.printf("collector=%s writers=%d snapshots=%d%n", name, writers, attempts);
        System.out.printf("broken snapshots: %d (%.2f%%)%n", broken, 100.0 * broken / attempts);
        System.out.printf("  sum(buckets) < count: %d%n", lessThanCount);
        System.out.printf("  sum(buckets) > count: %d%n", greaterThanCount);
        System.out.printf("record() calls: %d, final count: %d, delta: %d%n",
                totalCalls, finalSnapshot.count(), finalSnapshot.count() - totalCalls);
        System.out.printf("final sum(buckets): %d, matches final count: %b%n",
                finalBucketSum, finalBucketSum == finalSnapshot.count());
    }
}
