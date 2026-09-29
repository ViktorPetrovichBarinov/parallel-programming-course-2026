package lab1;

/**
 * Step 1 Empty lock in record()
 */
public class EmptyLockCollector implements MetricsCollector {

    private final Object lock = new Object();

    @Override
    public void record(long value) {
        synchronized (lock) {
        }
    }

    @Override
    public Snapshot snapshot() {
        synchronized (lock) {
            return new Snapshot(new long[Percentile.BUCKETS_NUMBER], 0, 0, Long.MAX_VALUE, 0, 0, 0);
        }
    }
}
