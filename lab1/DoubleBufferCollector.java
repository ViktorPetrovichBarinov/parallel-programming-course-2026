package lab1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step 4: double-buffering.
 */
public class DoubleBufferCollector implements MetricsCollector {

    private static final int NOWHERE = -1;

    static final class ThreadBuffers {
        final long[][] buckets = new long[2][Percentile.BUCKETS_NUMBER];
        final long[] count = new long[2];
        final long[] sum = new long[2];
        final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        final long[] max = {0, 0};

        // -1 = nowhere, 0 = write into buffer 0, 1 = write into buffer 1.
        final AtomicInteger inside = new AtomicInteger(NOWHERE);

        void write(int b, int bucket, long value) {
            buckets[b][bucket]++;
            count[b]++;
            sum[b] += value;
            if (value < min[b]) {
                min[b] = value;
            }
            if (value > max[b]) {
                max[b] = value;
            }
        }

        void clear(int b) {
            Arrays.fill(buckets[b], 0);
            count[b] = 0;
            sum[b] = 0;
            min[b] = Long.MAX_VALUE;
            max[b] = 0;
        }
    }

    private final Percentile percentile = new Percentile();
    private final boolean verifyActive;

    private final Object snapLock = new Object();
    private final List<ThreadBuffers> allStates = new ArrayList<>();

    //shared between threads var.
    private volatile int active = 0;

    private final long[] globalBuckets = new long[Percentile.BUCKETS_NUMBER];
    private long globalCount;
    private long globalSum;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax;

    private final ThreadLocal<ThreadBuffers> myBuffers = ThreadLocal.withInitial(() -> {
        ThreadBuffers buffers = new ThreadBuffers();
        synchronized (snapLock) {
            allStates.add(buffers);
        }
        return buffers;
    });

    public DoubleBufferCollector() {
        this(true);
    }

    public DoubleBufferCollector(boolean verifyActive) {
        this.verifyActive = verifyActive;
    }

    @Override
    public void record(long value) {
        ThreadBuffers buffers = myBuffers.get();

        int b;
        while (true) {
            b = active;
            buffers.inside.set(b);
            if (!verifyActive || active == b) {
                break;
            }
            buffers.inside.setRelease(NOWHERE);
        }

        int bucket = percentile.bucketOf(value);
        buffers.write(b, bucket, value);

        buffers.inside.setRelease(NOWHERE);
    }

    @Override
    public Snapshot snapshot() {
        synchronized (snapLock) {
            int old = active;
            active = 1 - old;

            for (ThreadBuffers buffers : allStates) {
                while (buffers.inside.get() == old) {
                    Thread.onSpinWait();
                }
            }

            for (ThreadBuffers buffers : allStates) {
                for (int i = 0; i < Percentile.BUCKETS_NUMBER; i++) {
                    globalBuckets[i] += buffers.buckets[old][i];
                }
                globalCount += buffers.count[old];
                globalSum += buffers.sum[old];
                globalMin = Math.min(globalMin, buffers.min[old]);
                globalMax = Math.max(globalMax, buffers.max[old]);
                buffers.clear(old);
            }

            long p50 = percentile.percentile50(globalCount, globalBuckets);
            long p99 = percentile.percentile99(globalCount, globalBuckets);

            long[] copy = Arrays.copyOf(globalBuckets, Percentile.BUCKETS_NUMBER);
            return new Snapshot(copy, globalCount, globalSum, globalMin, globalMax, p50, p99);
        }
    }
}
