package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;

public class ExceptionLogger {

    private final static Logger LOGGER = LoggerFactory.getLogger(ExceptionLogger.class);

    public static <T> Function<Throwable, ? extends T> get() {
        return (Function<Throwable, T>) throwable -> {
            LOGGER.error("Uncaught exception", throwable);
            return null;
        };
    }

}
