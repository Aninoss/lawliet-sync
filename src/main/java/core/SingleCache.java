package core;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SingleCache<T> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SingleCache.class);

    private Instant nextReset = null;
    private T value = null;

    public synchronized T get() {
        if (nextReset == null || Instant.now().isAfter(nextReset)) {
            return fetch();
        }

        return value;
    }

    public synchronized T getAsync() {
        if (value == null) {
            return get();
        }

        if (nextReset == null || Instant.now().isAfter(nextReset)) {
            resetUpdateTimer();
            GlobalThreadPool.getExecutorService().submit(this::fetch);
        }

        return value;
    }

    public void resetUpdateTimer() {
        nextReset = Instant.now().plus(getRefreshRateMinutes(), ChronoUnit.MINUTES);
    }

    public T fetch() {
        resetUpdateTimer();
        try {
            T newValue = fetchValue();
            if (newValue != null) {
                this.value = newValue;
            }
        } catch (Throwable e) {
            LOGGER.error("Uncaught exception", e);
        }
        return this.value;
    }

    protected int getRefreshRateMinutes() {
        return 5;
    }

    protected abstract T fetchValue();

}
