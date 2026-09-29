package lab1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Step3
 */
public class ThreadLocalCollector implements MetricsCollector {

    static final class ThreadState {
        final AtomicLongArray buckets = new AtomicLongArray(Percentile.BUCKETS_NUMBER);
        final AtomicLong count = new AtomicLong();
        final AtomicLong sum = new AtomicLong();
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong();
    }

    private final Percentile percentile = new Percentile();
    private final List<ThreadState> allStates = new ArrayList<>();
    private final Object listLock = new Object();

    private final ThreadLocal<ThreadState> myState = ThreadLocal.withInitial(() -> {
        ThreadState state = new ThreadState();
        synchronized (listLock) {
            allStates.add(state);
        }
        return state;
    });

    @Override
    public void record(long value) {
        ThreadState state = myState.get();
        int bucketIndex = percentile.bucketOf(value);

        state.buckets.setRelease(bucketIndex, state.buckets.getPlain(bucketIndex) + 1);
        state.count.setRelease(state.count.getPlain() + 1);
        state.sum.setRelease(state.sum.getPlain() + value);

        if (value < state.min.getPlain()) {
            state.min.setRelease(value);
        }
        if (value > state.max.getPlain()) {
            state.max.setRelease(value);
        }
    }

    @Override
    public Snapshot snapshot() {
        List<ThreadState> states;
        synchronized (listLock) {
            states = new ArrayList<>(allStates);
        }

        long[] buckets = new long[Percentile.BUCKETS_NUMBER];
        long count = 0;
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = 0;

        for (ThreadState state : states) {
            for (int i = 0; i < Percentile.BUCKETS_NUMBER; i++) {
                buckets[i] += state.buckets.get(i);
            }
            count += state.count.get();
            sum += state.sum.get();
            min = Math.min(min, state.min.get());
            max = Math.max(max, state.max.get());
        }

        long p50 = percentile.percentile50(count, buckets);
        long p99 = percentile.percentile99(count, buckets);

        return new Snapshot(buckets, count, sum, min, max, p50, p99);
    }
}
