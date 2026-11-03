package monitoring;

import java.util.Arrays;

public final class LateBindingAverage {
    private final long windowSec;
    private long[] ts;
    private double[] vals;
    private int n = 0;

    public LateBindingAverage(long windowSec, int initialCapacity) {
        this.windowSec = windowSec;
        this.ts = new long[initialCapacity];
        this.vals = new double[initialCapacity];
    }

    public void add(long t, double v) {
        if (n == ts.length) {
            ts = Arrays.copyOf(ts, n * 2);
            vals = Arrays.copyOf(vals, n * 2);
        }
        ts[n] = t;
        vals[n] = v;
        n++;
    }

    public double average(long now) {
        double s = 0.0;
        int c = 0;
        long from = now - windowSec;
        for (int i = 0; i < n; i++) {
            if (ts[i] >= from) {
                s += vals[i];
                c++;
            }
        }
        return c == 0 ? Double.NaN : s / c;
    }
}
