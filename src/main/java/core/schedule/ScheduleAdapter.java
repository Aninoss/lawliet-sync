package core.schedule;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleAdapter extends TimerTask {

    private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleAdapter.class);

    private final ScheduleInterface scheduleEvent;

    public ScheduleAdapter(ScheduleInterface scheduleEvent) {
        this.scheduleEvent = scheduleEvent;
    }

    @Override
    public void run() {
        try {
            scheduleEvent.run();
        } catch (Throwable throwable) {
            LOGGER.error("Scheduled event failed", throwable);
        }
    }

}
