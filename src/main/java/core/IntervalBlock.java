package core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import core.util.TimeUtil;

public class IntervalBlock {

    private final int time;
    private final ChronoUnit chronoUnit;
    Instant nextRequest;

    public IntervalBlock(int time, ChronoUnit chronoUnit) {
        this.time = time;
        this.chronoUnit = chronoUnit;

        nextRequest = Instant.now().plus(time, chronoUnit);
    }

    public boolean blockInterruptable() throws InterruptedException {
        long wait = TimeUtil.getMilisBetweenInstants(Instant.now(), nextRequest);
        Thread.sleep(wait);
        nextRequest = Instant.now().plus(time, chronoUnit);
        return true;
    }

    public boolean block() {
        try {
            long wait = TimeUtil.getMilisBetweenInstants(Instant.now(), nextRequest);
            Thread.sleep(wait);
            nextRequest = Instant.now().plus(time, chronoUnit);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }


}
