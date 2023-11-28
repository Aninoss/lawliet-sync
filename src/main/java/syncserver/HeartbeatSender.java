package syncserver;

import core.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatSender {

    private final static Logger LOGGER = LoggerFactory.getLogger(HeartbeatSender.class);
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static boolean started = false;

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;

        executorService.scheduleAtFixedRate(() -> {
            try {
                for (Cluster cluster : ClusterConnectionManager.getClusters()) {
                    cluster.send(EventOut.HEARTBEAT).exceptionally(ExceptionLogger.get());
                }
            } catch (Throwable e) {
                LOGGER.error("Error while sending heartbeat", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

}