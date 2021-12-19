package syncserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketJsonClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckOwnConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(CheckOwnConnection.class);

    private static WebSocketJsonClient client = null;

    public static void startScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (client == null) {
                    client = new WebSocketJsonClient("localhost", 9998, "sync", System.getenv("SYNC_AUTH"));
                    client.connectBlocking(5, TimeUnit.SECONDS);
                }
                client.send("PING", new JSONObject()).get(5, TimeUnit.SECONDS);
            } catch (Throwable e) {
                Runtime.getRuntime().halt(2);
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

}
