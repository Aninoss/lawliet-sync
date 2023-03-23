package core.payments.paddle;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import core.Program;
import mysql.modules.paddlesubscriptions.DBPaddleSubscriptions;
import mysql.modules.paddlesubscriptions.PaddleData;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaddleCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleCache.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final HashMap<Long, PaddleSubscription> subscriptionMap = new HashMap<>();

    private static boolean modified = false;

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

    public static synchronized void reload(long subId) throws IOException {
        if (Program.isProductionMode()) {
            modified = false;
            Map<Long, PaddleData> paddleDBMap = DBPaddleSubscriptions.retrievePaddleSubscriptionMap();
            List<JSONObject> subscriptions = subId > 0
                    ? PaddleAPI.retrieveSubscriptions(subId)
                    : PaddleAPI.retrieveSubscriptions();

            if (modified) {
                LOGGER.info("Paddle load cancelled to prevent race condition");
                return;
            }

            if (subId > 0) {
                subscriptionMap.remove(subId);
            } else {
                subscriptionMap.clear();
            }
            for (JSONObject json : subscriptions) {
                PaddleSubscription paddleSubscription = extractPaddleSubscription(paddleDBMap, json);
                if (paddleSubscription != null) {
                    subscriptionMap.put(paddleSubscription.getSubId(), paddleSubscription);
                }
            }
        }

        LOGGER.info("Paddle load successful ({})", subscriptionMap.size());
    }

    public static void put(PaddleSubscription paddleSubscription) {
        modified = true;
        subscriptionMap.put(paddleSubscription.getSubId(), paddleSubscription);
    }

    public static Collection<PaddleSubscription> getSubscriptions() {
        return Collections.unmodifiableCollection(subscriptionMap.values());
    }

    public static Map<Long, PaddleSubscription> getSubscriptionMap() {
        return Collections.unmodifiableMap(subscriptionMap);
    }

    private static PaddleSubscription extractPaddleSubscription(Map<Long, PaddleData> paddleDBMap, JSONObject json) {
        long subId = json.getLong("subscription_id");
        PaddleData data = paddleDBMap.get(subId);
        if (data != null) {
            return new PaddleSubscription(
                    data.getSubId(),
                    json.getLong("plan_id"),
                    data.getUserId(),
                    data.unlocksServer(),
                    json.has("quantity") ? json.getInt("quantity") : 1,
                    json.getString("state"),
                    String.format("%s %.02f", json.getJSONObject("last_payment").getString("currency"), json.getJSONObject("last_payment").getDouble("amount")),
                    json.has("next_payment") ? LocalDate.parse(json.getJSONObject("next_payment").getString("date")) : null,
                    json.getString("update_url")
            );
        } else {
            return null;
        }
    }

}
