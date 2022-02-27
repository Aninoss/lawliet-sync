package core.payments.paddle;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import mysql.modules.paddlesubscriptions.DBPaddleSubscriptions;
import mysql.modules.paddlesubscriptions.PaddleData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaddleCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleCache.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final HashMap<Integer, PaddleSubscription> subscriptionMap = new HashMap<>();

    public static void startScheduler() throws IOException {
        reload();
        executor.scheduleAtFixedRate(() -> {
            try {
                reload();
            } catch (Throwable e) {
                LOGGER.error("Paddle error", e);
            }
        }, 20, 20, TimeUnit.MINUTES);
    }

    public static synchronized void reload() throws IOException {
        reload(0);
    }

    public static synchronized void reload(int subId) throws IOException {
        Map<Integer, PaddleData> paddleDBMap = DBPaddleSubscriptions.retrievePaddleSubscriptionMap();
        List<JSONObject> subscriptions;
        if (subId > 0) {
            subscriptions = PaddleAPI.retrieveSubscriptions(subId);
            subscriptionMap.remove(subId);
        } else {
            subscriptions = PaddleAPI.retrieveSubscriptions();
            subscriptionMap.clear();
        }
        for (JSONObject json : subscriptions) {
            PaddleSubscription paddleSubscription = extractPaddleSubscription(paddleDBMap, json);
            if (paddleSubscription != null) {
                subscriptionMap.put(paddleSubscription.getSubId(), paddleSubscription);
            }
        }

        LOGGER.info("Paddle load successful ({})", subscriptionMap.size());
    }

    public static Collection<PaddleSubscription> getSubscriptions() {
        return Collections.unmodifiableCollection(subscriptionMap.values());
    }

    private static PaddleSubscription extractPaddleSubscription(Map<Integer, PaddleData> paddleDBMap, JSONObject json) {
        int subId = json.getInt("subscription_id");
        PaddleData data = paddleDBMap.get(subId);
        if (data != null) {
            return new PaddleSubscription(
                    data.getSubId(),
                    json.getInt("plan_id"),
                    data.getUserId(),
                    data.unlocksServer(),
                    json.has("quantity") ? json.getInt("quantity") : 1,
                    json.getString("state"),
                    String.format("%s %.02f", json.getJSONObject("last_payment").getString("currency"), json.getJSONObject("last_payment").getDouble("amount")),
                    json.has("next_payment") ? LocalDate.parse(json.getJSONObject("next_payment").getString("date")) : null
            );
        } else {
            return null;
        }
    }

}
