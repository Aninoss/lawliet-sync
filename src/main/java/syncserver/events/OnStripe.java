package syncserver.events;

import core.payments.PremiumManager;
import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SendEvent;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "STRIPE")
public class OnStripe implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        JSONObject jsonObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getInstance().getActiveClusters()
                .forEach(c -> SendEvent.sendJSON(
                        "PATREON",
                        c.getClusterId(),
                        jsonObject
                ));

        long userId = dataJson.getLong("user_id");
        String title = dataJson.getString("title");
        String desc = dataJson.getString("desc");
        ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster().ifPresent(cluster -> {
            SendEvent.sendUserNotification(
                    cluster.getClusterId(),
                    userId,
                    title,
                    desc,
                    null,
                    null,
                    null,
                    null
            );
        });

        return null;
    }

}