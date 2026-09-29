package lab1;

import java.util.Arrays;

/**
 * Step 1: one lock for all record.
 */
public class SynchronizedCollector implements MetricsCollector {

    private final long[] buckets = new long[Percentile.BUCKETS_NUMBER];
    private final Percentile percentile = new Percentile();
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max = 0;

    @Override
    public synchronized void record(long value) {
        buckets[percentile.bucketOf(value)]++;
        count++;
        sum += value;
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
    }

    @Override
    public synchronized Snapshot snapshot() {
        long[] copy = Arrays.copyOf(buckets, Percentile.BUCKETS_NUMBER);
        long p50 = percentile.percentile50(count, copy);
        long p99 = percentile.percentile99(count, copy);
        return new Snapshot(copy, count, sum, min, max, p50, p99);
    }
}
