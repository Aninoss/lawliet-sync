package syncserver.events;

import core.payments.paddle.PaddleManager;
import core.payments.paddle.PaddleSubscription;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SUB_ACTIVE")
public class OnSubscriptionActive implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long subId = jsonObject.getLong("sub_id");
        PaddleSubscription paddleSubscription = PaddleManager.retrieveSubscriptionBySubId(subId);
        boolean active = paddleSubscription != null && paddleSubscription.getStatus().equals("active");

        JSONObject resultJson = new JSONObject();
        resultJson.put("active", active);
        return resultJson;
    }

}