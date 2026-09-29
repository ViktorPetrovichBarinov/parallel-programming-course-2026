package lab1;

import java.util.Random;

/**
 * Load generator: 2^20 values in range 1..1023 by Zipf
 * with exponent 1.15 and seed.
 */
public final class ZipfGenerator {
    static final int MIN_VALUE = 1;
    static final int MAX_VALUE = 1023;
    static final double EXPONENT = 1.15;
    static final int DEFAULT_SIZE = 1 << 20;
    static final long DEFAULT_SEED = 42L;

    private ZipfGenerator() {
    }

    public static long[] generate() {
        return generate(DEFAULT_SIZE, DEFAULT_SEED);
    }

    public static long[] generate(int size, long seed) {
        double[] cdf = buildCdf();
        Random random = new Random(seed);
        long[] values = new long[size];
        for (int i = 0; i < size; i++) {
            values[i] = MIN_VALUE + sample(cdf, random.nextDouble());
        }
        return values;
    }

    private static double[] buildCdf() {
        int n = MAX_VALUE - MIN_VALUE + 1;
        double[] cdf = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += Math.pow(i + MIN_VALUE, -EXPONENT);
            cdf[i] = total;
        }
        for (int i = 0; i < n; i++) {
            cdf[i] /= total;
        }
        return cdf;
    }

    private static int sample(double[] cdf, double u) {
        int lo = 0;
        int hi = cdf.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cdf[mid] > u) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }
}
