package core;

public class SyncedRatelimitManager {

    private static final SyncedRatelimitManager ourInstance = new SyncedRatelimitManager();

    public static SyncedRatelimitManager getInstance() {
        return ourInstance;
    }

    private SyncedRatelimitManager() {
    }

    private long intervalTimeNanos = 21_000_000L;
    private long nextRequest = 0;

    public synchronized long processWaitingTimeNanos() {
        long currentTime = System.nanoTime();
        long waitingTime = Math.max(0, nextRequest - currentTime);
        this.nextRequest = Math.max(currentTime + intervalTimeNanos, this.nextRequest + intervalTimeNanos);
        return waitingTime;
    }

    public void setIntervalTimeNanos(long intervalTimeNanos) {
        this.intervalTimeNanos = intervalTimeNanos;
    }

}
