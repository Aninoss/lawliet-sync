package syncserver.events;

import core.ExceptionLogger;
import core.payments.PremiumManager;
import core.payments.paddlebilling.PaddleBillingCache;
import core.payments.paddlebilling.PaddleBillingSubscription;
import mysql.modules.premium.DBPremium;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

import java.util.ArrayList;

@SyncServerEvent(event = "PADDLE_BILLING")
public class OnPaddleBilling implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnPaddleBilling.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        PaddleBillingSubscription subscription = PaddleBillingSubscription.fromJson(jsonObject.getJSONObject("data"));
        LOGGER.info("New subscription received: {}", subscription.getSubscriptionId());
        PaddleBillingCache.put(subscription);

        boolean created = jsonObject.getBoolean("created");
        if (created && subscription.getPresetGuilds() != null) {
            ArrayList<Long> presetGuilds = new ArrayList<>(subscription.getPresetGuilds());
            while (presetGuilds.size() > subscription.getQuantity()) {
                presetGuilds.remove(presetGuilds.size() - 1);
            }

            int currentSlot = PremiumManager.retrieveUnlockServersNumber(subscription.getUserId()) - presetGuilds.size();
            for (int i = 0; i < presetGuilds.size(); i++) {
                DBPremium.modify(subscription.getUserId(), currentSlot++, presetGuilds.get(i));
            }
        }

        JSONObject jsonPremiumObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getClusters()
                .forEach(c -> c.send(EventOut.PATREON, jsonPremiumObject).exceptionally(ExceptionLogger.get()));

        if (created) {
            ClusterConnectionManager.getFirstFullyConnectedPublicCluster().ifPresent(cluster -> {
                SyncUtil.sendUserNotification(
                        cluster,
                        subscription.getUserId(),
                        jsonObject.getString("title"),
                        jsonObject.getString("description"),
                        null,
                        null,
                        null,
                        null,
                        30_000
                ).exceptionally(ExceptionLogger.get());
            });
        }

        return null;
    }

}