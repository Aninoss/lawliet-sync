package core.payments.paddle;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import mysql.modules.paddlesubscriptions.DBPaddleSubscriptions;
import mysql.modules.paddlesubscriptions.PaddleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaddleCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleCache.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static List<PaddleSubscription> subscriptions = Collections.emptyList();

    public static void startScheduler() throws IOException {
        reload();
        executor.scheduleAtFixedRate(() -> {
            try {
                reload();
            } catch (Throwable e) {
                LOGGER.error("Paddle error", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public static void reload() throws IOException {
        Map<Integer, PaddleData> paddleDBMap = DBPaddleSubscriptions.retrievePaddleSubscriptionMap();
        subscriptions = PaddleAPI.retrieveSubscriptions().stream()
                .map(json -> {
                    int subId = json.getInt("subscription_id");
                    PaddleData data = paddleDBMap.get(subId);
                    if (data != null) {
                        return new PaddleSubscription(
                                data.getSubId(),
                                json.getInt("plan_id"),
                                data.getUserId(),
                                data.unlocksServer(),
                                json.getInt("quantity"),
                                json.getString("state"),
                                String.format("%s %.02f", json.getJSONObject("last_payment").getString("currency"), json.getJSONObject("last_payment").getDouble("amount")),
                                json.has("next_payment") ? LocalDate.parse(json.getJSONObject("next_payment").getString("date")) : null
                        );
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LOGGER.info("Paddle load successful ({})", subscriptions.size());
    }

    public static List<PaddleSubscription> getSubscriptions() {
        return subscriptions;
    }

}
