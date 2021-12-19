package syncserver.events;

import com.stripe.exception.StripeException;
import core.payments.PremiumManager;
import core.payments.StripeCache;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.ClusterConnectionManager;
import syncserver.SendEvent;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "STRIPE")
public class OnStripe implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnStripe.class);

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        LOGGER.info("New subscription received");
        try {
            StripeCache.reload();
        } catch (StripeException e) {
            LOGGER.error("Stripe error");
        }

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
                    null,
                    60_000
            );
        });

        return null;
    }

}