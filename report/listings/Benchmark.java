package monitoring;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public final class Benchmark {
    static final long WINDOW_SEC = 300;
    static final int[] SIZES = {10_000, 50_000, 100_000, 500_000, 1_000_000, 5_000_000};
    static final int TRIALS = 9;
    static final long WORK_PER_TRIAL = 60_000_000L;
    static final int EARLY_UPDATES_PER_TRIAL = 1_000_000;

    private static double sink = 0;

    private static double reading(int i) {
        return 60 + (i % 40);
    }

    static void sanityCheck() {
        LateBindingAverage late = new LateBindingAverage(WINDOW_SEC, 1024);
        EarlyBindingAverage early = new EarlyBindingAverage(WINDOW_SEC, 1024);
        for (int i = 0; i < 5_000; i++) {
            late.add(i, reading(i));
            double e = early.add(i, reading(i));
            double l = late.average(i);
            if (Math.abs(e - l) > 1e-6) {
                throw new IllegalStateException("Mismatch at " + i + ": " + e + " vs " + l);
            }
        }
        System.out.println("Sanity check passed: early and late binding agree.");
    }

    private static double median(double[] a) {
        double[] c = a.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    private static double timeLate(LateBindingAverage late, long now, int reps) {
        long t0 = System.nanoTime();
        for (int k = 0; k < reps; k++) {
            sink += late.average(now);
        }
        return (System.nanoTime() - t0) / 1e6 / reps;
    }

    public static void run(Path outDir) throws IOException {
        sanityCheck();
        Files.createDirectories(outDir);

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(outDir.resolve("benchmark.csv")))) {
            w.println("readings,late_ms,early_ms");
            for (int n : SIZES) {
                LateBindingAverage late = new LateBindingAverage(WINDOW_SEC, n);
                for (int i = 0; i < n; i++) {
                    late.add(i, reading(i));
                }
                long now = n - 1;
                int reps = (int) Math.max(5, WORK_PER_TRIAL / n);
                timeLate(late, now, reps);
                double[] lateTrials = new double[TRIALS];
                for (int i = 0; i < TRIALS; i++) {
                    lateTrials[i] = timeLate(late, now, reps);
                }
                double lateMs = median(lateTrials);

                EarlyBindingAverage early = new EarlyBindingAverage(WINDOW_SEC, 1024);
                for (long i = Math.max(0, n - 400); i < n; i++) {
                    early.add(i, reading((int) i));
                }
                long t = n;
                for (int k = 0; k < EARLY_UPDATES_PER_TRIAL; k++) {
                    sink += early.add(t++, 90);
                }
                double[] earlyTrials = new double[TRIALS];
                for (int i = 0; i < TRIALS; i++) {
                    long t0 = System.nanoTime();
                    for (int k = 0; k < EARLY_UPDATES_PER_TRIAL; k++) {
                        sink += early.add(t++, 90);
                    }
                    earlyTrials[i] = (System.nanoTime() - t0) / 1e6 / EARLY_UPDATES_PER_TRIAL;
                }
                double earlyMs = median(earlyTrials);

                w.printf(Locale.ROOT, "%d,%.6f,%.9f%n", n, lateMs, earlyMs);
                System.out.printf(Locale.ROOT,
                        "n=%,10d  late=%10.4f ms  early=%.7f ms%n", n, lateMs, earlyMs);
            }
        }
        System.out.println("checksum (ignore): " + sink);
    }
}
