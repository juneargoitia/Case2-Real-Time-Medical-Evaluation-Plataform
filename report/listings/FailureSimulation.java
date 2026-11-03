package monitoring;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

public final class FailureSimulation {
    static final int PATIENTS = 3;
    static final int DURATION_SEC = 600;
    static final int FAIL_PATIENT = 2;
    static final int FAIL_TIME = 300;
    static final int WATCHDOG_TIMEOUT_SEC = 10;
    static final long SHORT_WINDOW_SEC = 5;
    static final double ALERT_ON = 120.0;
    static final double ALERT_OFF = 110.0;

    private static double heartRate(int p, int t, Random rnd) {
        double base = 72 + 8 * Math.sin(t / 40.0 + p) + rnd.nextGaussian() * 2;
        if (p == 1 && t >= 200 && t < 260) {
            base = 135 + rnd.nextGaussian() * 3;
        }
        return base;
    }

    public static void run(Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Random rnd = new Random(42);
        EarlyBindingAverage[] avg = new EarlyBindingAverage[PATIENTS];
        boolean[] inAlert = new boolean[PATIENTS];
        for (int p = 0; p < PATIENTS; p++) {
            avg[p] = new EarlyBindingAverage(SHORT_WINDOW_SEC, 64);
        }
        Watchdog watchdog = new Watchdog(PATIENTS, WATCHDOG_TIMEOUT_SEC);

        try (PrintWriter sim = new PrintWriter(Files.newBufferedWriter(outDir.resolve("simulation.csv")));
             PrintWriter alerts = new PrintWriter(Files.newBufferedWriter(outDir.resolve("alerts.csv")))) {
            sim.println("time_s,patient,heart_rate,avg_5s");
            alerts.println("time_s,patient,type,detail");

            for (int t = 1; t <= DURATION_SEC; t++) {
                for (int p = 0; p < PATIENTS; p++) {
                    boolean deviceAlive = !(p == FAIL_PATIENT && t >= FAIL_TIME);
                    if (deviceAlive) {
                        double hr = heartRate(p, t, rnd);
                        double a = avg[p].add(t, hr);
                        watchdog.heartbeat(p, t);
                        sim.printf(Locale.ROOT, "%d,%d,%.2f,%.2f%n", t, p + 1, hr, a);

                        if (!inAlert[p] && a > ALERT_ON) {
                            inAlert[p] = true;
                            alerts.printf(Locale.ROOT, "%d,%d,THRESHOLD,avg_5s=%.1f bpm%n", t, p + 1, a);
                        } else if (inAlert[p] && a <= ALERT_OFF) {
                            inAlert[p] = false;
                        }
                    } else {
                        sim.printf(Locale.ROOT, "%d,%d,,%n", t, p + 1);
                    }
                    if (watchdog.check(p, t)) {
                        alerts.printf(Locale.ROOT, "%d,%d,DEVICE_OFFLINE,no reading for >%d s (last seen t=%d)%n",
                                t, p + 1, WATCHDOG_TIMEOUT_SEC, watchdog.lastSeen(p));
                    }
                }
            }
        }
        System.out.println("Simulation written to " + outDir.resolve("simulation.csv")
                + " and " + outDir.resolve("alerts.csv"));
    }
}
