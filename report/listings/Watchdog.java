package monitoring;

public final class Watchdog {
    private final long timeoutSec;
    private final long[] lastSeen;
    private final boolean[] alerted;

    public Watchdog(int patients, long timeoutSec) {
        this.timeoutSec = timeoutSec;
        this.lastSeen = new long[patients];
        this.alerted = new boolean[patients];
    }

    public void heartbeat(int patient, long t) {
        lastSeen[patient] = t;
        alerted[patient] = false;
    }

    public boolean check(int patient, long now) {
        if (!alerted[patient] && now - lastSeen[patient] > timeoutSec) {
            alerted[patient] = true;
            return true;
        }
        return false;
    }

    public long lastSeen(int patient) {
        return lastSeen[patient];
    }
}
