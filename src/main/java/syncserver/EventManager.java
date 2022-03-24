package syncserver;

import java.net.URI;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    private static final HashMap<String, SyncServerFunction> eventMap = new HashMap<>();

    public static void register() {
        registerEvents();
        registerRestService();
    }

    public static SyncServerFunction getEvent(String name) {
        return eventMap.get(name);
    }

    private static void registerEvents() {
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
                .forEach(EventManager::addEvent);
    }

    private static void registerRestService() {
        ResourceConfig rc = new ResourceConfig(RestService.class, AuthFilter.class);

        URI endpoint = UriBuilder
                .fromUri("http://0.0.0.0/api/")
                .port(Integer.parseInt(System.getenv("SYNC_SERVER_PORT")))
                .build();

        GrizzlyHttpServerFactory.createHttpServer(endpoint, rc);
    }

    private static void addEvent(SyncServerFunction function) {
        SyncServerEvent event = function.getClass().getAnnotation(SyncServerEvent.class);
        if (event != null) {
            eventMap.put(event.event(), function);
        }
    }

}
