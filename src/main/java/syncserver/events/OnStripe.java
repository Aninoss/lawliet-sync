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
    public JSONObject apply(String socketId, JSONObject dataJson) {
        LOGGER.info("New subscription received");

        long userId = dataJson.getLong("user_id");
        int subId = dataJson.has("sub_id") ? dataJson.getInt("sub_id") : 0;
        if (subId != 0) {
            boolean unlocksServer = dataJson.getBoolean("unlocks_server");
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

        JSONObject jsonObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getInstance().getActiveClusters()
                .forEach(c -> SendEvent.sendJSON(
                        "PATREON",
                        c.getClusterId(),
                        jsonObject
                ));

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