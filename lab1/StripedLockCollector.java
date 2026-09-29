package lab1;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Step 2: lock sharding.
 */
public class StripedLockCollector implements MetricsCollector {

    static final int GROUPS = 16;

    private final Percentile percentile = new Percentile();
    private final long[] buckets = new long[Percentile.BUCKETS_NUMBER];
    private final Object[] locks = new Object[GROUPS];

    private final AtomicLong count = new AtomicLong();
    private final AtomicLong sum = new AtomicLong();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong();

    public StripedLockCollector() {
        for (int i = 0; i < GROUPS; i++) {
            locks[i] = new Object();
        }
    }

    @Override
    public void record(long value) {
        int bucketIndex = percentile.bucketOf(value);
        synchronized (locks[bucketIndex % GROUPS]) {
            buckets[bucketIndex]++;
        }

        count.incrementAndGet();
        sum.addAndGet(value);

        long current;
        do {
            current = min.get();
            if (value >= current) {
                break;
            }
        } while (!min.compareAndSet(current, value));

        do {
            current = max.get();
            if (value <= current) {
                break;
            }
        } while (!max.compareAndSet(current, value));
    }

    @Override
    public Snapshot snapshot() {
        long[] copy = new long[Percentile.BUCKETS_NUMBER];
        for (int group = 0; group < GROUPS; group++) {
            synchronized (locks[group]) {
                for (int i = group; i < Percentile.BUCKETS_NUMBER; i += GROUPS) {
                    copy[i] = buckets[i];
                }
            }
        }

        long countSnapshot = count.get();
        long p50 = percentile.percentile50(countSnapshot, copy);
        long p99 = percentile.percentile99(countSnapshot, copy);

        return new Snapshot(copy, countSnapshot, sum.get(), min.get(), max.get(), p50, p99);
    }
}
