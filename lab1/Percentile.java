package lab1;

public class Percentile {

    public static final int BUCKETS_NUMBER = 256;
    public static final int BUCKET_SIZE = 4;
    public static final int MAX_VALUE = BUCKETS_NUMBER * BUCKET_SIZE - 1;

    private static final double PERCENTILE_50 = 0.50;
    private static final double PERCENTILE_99 = 0.99;

    public int bucketOf(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(
                "Value must be more than -1; value: " + value
            );
        }
        if (value >= MAX_VALUE) {
            return BUCKETS_NUMBER - 1;
        }
        return (int) (value / BUCKET_SIZE);
    }

    public long percentile50(long count, long[] bucket) {
        return percentile(count, bucket, PERCENTILE_50);
    }

    public long percentile99(long count, long[] bucket) {
        return percentile(count, bucket, PERCENTILE_99);
    }

    public long percentile(long count, long[] bucket, double percentile) {
        if (count <= 0) {
            return 0;
        }
        long limit = (long) Math.ceil(count * percentile);
        long cumulative = 0;
        for (int i = 0; i < BUCKETS_NUMBER; i++) {
            cumulative += bucket[i];
            if (cumulative >= limit) {
                return (long) i * BUCKET_SIZE;
            }
        }
        return (long) (BUCKETS_NUMBER - 1) * BUCKET_SIZE;
    }
}
