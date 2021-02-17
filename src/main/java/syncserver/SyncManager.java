package syncserver;

import org.java_websocket.server.WebSocketJsonServer;
import org.java_websocket.server.WebSocketServer;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;

public class SyncManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(SyncManager.class);

    private static final SyncManager ourInstance = new SyncManager();

    public static SyncManager getInstance() {
        return ourInstance;
    }

    private final WebSocketJsonServer server;
    private boolean started = false;

    private SyncManager() {
        server = new WebSocketJsonServer(new InetSocketAddress(9998), System.getenv("SYNC_AUTH"));
        server.addConnectedHandler(new OnConnected());
        server.addDisconnectedHandler(new OnDisconnected());

        Reflections reflections = new Reflections("syncserver/events");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SyncServerEvent.class);
        annotated.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.error("Error when creating sync event class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof SyncServerFunction)
                .map(obj -> (SyncServerFunction) obj)
                .forEach(this::addEvent);
    }

    public synchronized void start() {
        if (started)
            return;
        started = true;

        this.server.start();
        LOGGER.info("Waiting for clusters");
    }

    public WebSocketJsonServer getServer() {
        return server;
    }

    private void addEvent(SyncServerFunction function) {
        SyncServerEvent event = function.getClass().getAnnotation(SyncServerEvent.class);
        if (event != null)
            this.server.addEventHandler(event.event(), function);
    }

}
