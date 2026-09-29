package lab1;

public class Percentile {
    public final int BUCKETS_NUMBER = 256;
    public final int BUCKET_SIZE = 4;
    public final int MAX_VALUE = BUCKETS_NUMBER * BUCKET_SIZE - 1;
    private final double PERCENTILE_99 = 0.99;
    private final double PERCENTILE_50 = 0.50;

    public int bucketOf(long value) throws IllegalArgumentException {
        if (value < 0) {
            throw new IllegalArgumentException(
                "Value must be more than -1; value: " + value
            );
        }
        int index;
        if (value >= MAX_VALUE) {
            index = BUCKETS_NUMBER - 1;
        } else {
            index  = (int) (value / BUCKET_SIZE);
        }
        return index;
    }

    public long calculatePercentile99(long count, long[] bucket) {
        return percentileCalculation(count, bucket, PERCENTILE_99);
    }

    public long calculatePercentile50(long count, long[] bucket) {
        return percentileCalculation(count, bucket, PERCENTILE_50);
    }

    private long percentileCalculation(long count, long[] bucket, double percentile) {
        long limit = (long)Math.ceil(count * percentile);
        long cumulative = 0;
        for (int i = 0; i < BUCKETS_NUMBER; i++) {
            cumulative += bucket[i];
            if (cumulative >= limit) {
                return i * BUCKET_SIZE;
            }
        }

        return MAX_VALUE;
    }
}
