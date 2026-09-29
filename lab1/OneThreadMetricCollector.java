package lab1;

import java.util.Arrays;

public class OneThreadMetricCollector implements MetricsCollector {
    private long[] bucket = new long[Percentile.BUCKETS_NUMBER];
    private final Percentile percentile = new Percentile();
    private long count = 0;
    private long sum = 0;
    private long min = Long.MAX_VALUE;
    private long max = 0;
    @Override
    public void record(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                "Value must be more than or equal 0; value: " + value
            );
        }
        count++;
        sum += value;
        int bucketIndex = percentile.bucketOf(value);
        bucket[bucketIndex]++;
        min = Math.min(value, min);
        max = Math.max(value, max);
    }

    @Override
    public Snapshot snapshot() {
        long[] bucketCopy = Arrays.copyOf(bucket, Percentile.BUCKETS_NUMBER);
        long p99 = percentile.percentile99(count, bucketCopy);
        long p50 = percentile.percentile50(count, bucketCopy);
        Snapshot snapshot = new Snapshot(
            bucketCopy, count, sum,
            min, max, p50, p99
        );
        return snapshot;
    }
    
}
