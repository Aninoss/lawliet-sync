package core.payments.paddlebilling;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PaddleBillingCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleBillingCache.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final HashMap<String, PaddleBillingSubscription> subscriptionMap = new HashMap<>();

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
        reload(null);
    }

    public static synchronized void reload(String subscriptionId) throws IOException {
        modified = false;
        List<JSONObject> subscriptions = PaddleBillingAPI.retrieveSubscriptions(subscriptionId);

        if (modified) {
            LOGGER.info("Paddle load cancelled to prevent race condition");
            return;
        }

        if (subscriptionId != null) {
            subscriptionMap.remove(subscriptionId);
        } else {
            subscriptionMap.clear();
        }
        for (JSONObject json : subscriptions) {
            try {
                PaddleBillingSubscription paddleBillingSubscription = PaddleBillingSubscription.fromJson(json);
                subscriptionMap.put(paddleBillingSubscription.getSubscriptionId(), paddleBillingSubscription);
            } catch (JSONException e) {
                LOGGER.error("Could not create paddle billing object:\n{}", json, e);
            }
        }

        LOGGER.info("Paddle billing load successful ({})", subscriptionMap.size());
    }

    public static void put(PaddleBillingSubscription paddleBillingSubscription) {
        modified = true;
        subscriptionMap.put(paddleBillingSubscription.getSubscriptionId(), paddleBillingSubscription);
    }

    public static Collection<PaddleBillingSubscription> getSubscriptions() {
        return Collections.unmodifiableCollection(subscriptionMap.values());
    }

    public static Map<String, PaddleBillingSubscription> getSubscriptionMap() {
        return Collections.unmodifiableMap(subscriptionMap);
    }

}
