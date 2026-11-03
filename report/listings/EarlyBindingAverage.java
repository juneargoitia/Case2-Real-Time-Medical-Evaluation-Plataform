package monitoring;

public final class EarlyBindingAverage {
    private final long windowSec;
    private final long[] ts;
    private final double[] vals;
    private int head = 0;
    private int size = 0;
    private double sum = 0.0;

    public EarlyBindingAverage(long windowSec, int capacity) {
        this.windowSec = windowSec;
        this.ts = new long[capacity];
        this.vals = new double[capacity];
    }

    public double add(long t, double v) {
        while (size > 0 && ts[head] < t - windowSec) {
            evictOldest();
        }
        if (size == ts.length) {
            evictOldest();
        }
        int tail = (head + size) % ts.length;
        ts[tail] = t;
        vals[tail] = v;
        size++;
        sum += v;
        return sum / size;
    }

    public double average() {
        return size == 0 ? Double.NaN : sum / size;
    }

    private void evictOldest() {
        sum -= vals[head];
        head = (head + 1) % ts.length;
        size--;
    }
}
