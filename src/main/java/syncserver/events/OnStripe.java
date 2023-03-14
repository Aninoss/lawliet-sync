package syncserver.events;

import com.stripe.exception.StripeException;
import core.ExceptionLogger;
import core.payments.PremiumManager;
import core.payments.stripe.StripeCache;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "STRIPE")
public class OnStripe implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnStripe.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        LOGGER.info("New subscription received");

        long userId = jsonObject.getLong("user_id");
        try {
            StripeCache.reload();
        } catch (StripeException e) {
            LOGGER.error("Stripe error");
        }

        JSONObject jsonPremiumObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getClusters()
                .forEach(c -> c.send(EventOut.PATREON, jsonPremiumObject).exceptionally(ExceptionLogger.get()));

        String title = jsonObject.getString("title");
        String desc = jsonObject.getString("desc");
        ClusterConnectionManager.getFirstFullyConnectedPublicCluster().ifPresent(cluster -> {
            SyncUtil.sendUserNotification(
                    cluster,
                    userId,
                    title,
                    desc,
                    null,
                    null,
                    null,
                    null,
                    30_000
            ).exceptionally(ExceptionLogger.get());
        });

        return null;
    }

}