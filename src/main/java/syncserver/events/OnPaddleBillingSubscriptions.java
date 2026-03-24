package syncserver.events;

import core.payments.paddlebilling.PaddleBillingCache;
import core.payments.paddlebilling.PaddleBillingManager;
import core.payments.paddlebilling.PaddleBillingSubscription;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.io.IOException;

@SyncServerEvent(event = "PADDLE_BILLING_SUBSCRIPTIONS")
public class OnPaddleBillingSubscriptions implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        if (jsonObject.has("reload_subscription_id")) {
            try {
                PaddleBillingCache.reload(jsonObject.getString("reload_subscription_id"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JSONArray subsJson = new JSONArray();
        for (PaddleBillingSubscription subscription : PaddleBillingManager.retrieveActiveSubscriptionsByUserId(userId)) {
            JSONObject subJson = new JSONObject();
            subJson.put("subscription_id", subscription.getSubscriptionId());
            subJson.put("customer_id", subscription.getCustomerId());
            subJson.put("quantity", subscription.getQuantity());
            subJson.put("status", subscription.getStatus());
            subJson.put("unlocks_guilds", subscription.getUnlocksGuilds());
            subsJson.put(subJson);
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("subscriptions", subsJson);
        return resultJson;
    }

}