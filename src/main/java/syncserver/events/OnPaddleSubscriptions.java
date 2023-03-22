package syncserver.events;

import java.io.IOException;
import core.payments.paddle.PaddleCache;
import core.payments.paddle.PaddleManager;
import core.payments.paddle.PaddleSubscription;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PADDLE_SUBS")
public class OnPaddleSubscriptions implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        if (jsonObject.has("reload_sub_id")) {
            try {
                PaddleCache.reload(jsonObject.getInt("reload_sub_id"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JSONArray subsJson = new JSONArray();
        for (PaddleSubscription subscription : PaddleManager.retrieveSubscriptionsByUserId(userId)) {
            JSONObject subJson = new JSONObject();
            subJson.put("sub_id", subscription.getSubId());
            subJson.put("plan_id", subscription.getPlanId());
            subJson.put("quantity", subscription.getQuantity());
            subJson.put("total_price", subscription.getTotalPrice());
            subJson.put("next_payment", subscription.getNextPayment());
            subJson.put("update_url", subscription.getUpdateUrl());
            subsJson.put(subJson);
        }

        JSONObject resultJson = new JSONObject();
        resultJson.put("subscriptions", subsJson);
        return resultJson;
    }

}