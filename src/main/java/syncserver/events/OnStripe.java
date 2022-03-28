package syncserver.events;

import java.io.IOException;
import java.sql.SQLException;
import com.stripe.exception.StripeException;
import core.payments.PremiumManager;
import core.payments.paddle.PaddleCache;
import core.payments.stripe.StripeCache;
import mysql.modules.paddlesubscriptions.DBPaddleSubscriptions;
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
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        LOGGER.info("New subscription received");

        long userId = jsonObject.getLong("user_id");
        int subId = jsonObject.has("sub_id") ? jsonObject.getInt("sub_id") : 0;
        if (subId != 0) {
            boolean unlocksServer = jsonObject.getBoolean("unlocks_server");
            try {
                DBPaddleSubscriptions.savePaddleSubscription(subId, userId, unlocksServer);
            } catch (SQLException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                PaddleCache.reload(subId);
            } catch (IOException e) {
                LOGGER.error("Paddle error");
            }
        } else {
            try {
                StripeCache.reload();
            } catch (StripeException e) {
                LOGGER.error("Stripe error");
            }
        }

        JSONObject jsonPremiumObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getClusters()
                .forEach(c -> SendEvent.sendJSON(
                        "PATREON",
                        c.getClusterId(),
                        jsonPremiumObject
                ));

        String title = jsonObject.getString("title");
        String desc = jsonObject.getString("desc");
        ClusterConnectionManager.getFirstFullyConnectedCluster().ifPresent(cluster -> {
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