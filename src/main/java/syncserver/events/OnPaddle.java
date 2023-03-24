package syncserver.events;

import java.sql.SQLException;
import java.time.LocalDate;
import core.ExceptionLogger;
import core.payments.PremiumManager;
import core.payments.paddle.PaddleCache;
import core.payments.paddle.PaddleSubscription;
import mysql.modules.paddlesubscriptions.DBPaddleSubscriptions;
import mysql.modules.premium.DBPremium;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "PADDLE")
public class OnPaddle implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnPaddle.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        LOGGER.info("New subscription received");

        long userId = jsonObject.getLong("user_id");
        long subId = jsonObject.has("sub_id") ? jsonObject.getLong("sub_id") : 0;
        boolean unlocksServer = jsonObject.getBoolean("unlocks_server");
        try {
            DBPaddleSubscriptions.savePaddleSubscription(subId, userId, unlocksServer);
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        PaddleSubscription paddleSubscription = generatePaddleSubscription(jsonObject);
        PaddleCache.put(paddleSubscription);

        if (jsonObject.has("preset_guilds")) {
            JSONArray presetGuilds = jsonObject.getJSONArray("preset_guilds");
            while (presetGuilds.length() > paddleSubscription.getQuantity()) {
                presetGuilds.remove(presetGuilds.length() - 1);
            }

            int currentSlot = PremiumManager.retrieveUnlockServersNumber(userId) - presetGuilds.length();
            for (int i = 0; i < presetGuilds.length(); i++) {
                long guildId = presetGuilds.getLong(i);
                DBPremium.modify(userId, currentSlot++, guildId);
            }
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

    private PaddleSubscription generatePaddleSubscription(JSONObject json) {
        return new PaddleSubscription(
                json.getLong("sub_id"),
                json.getLong("plan_id"),
                json.getLong("user_id"),
                json.getBoolean("unlocks_server"),
                json.getInt("quantity"),
                json.getString("state"),
                json.getString("total_price"),
                LocalDate.parse(json.getString("next_payment")),
                json.getString("update_url")
        );
    }

}