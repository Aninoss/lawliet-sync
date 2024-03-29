package syncserver.events;

import core.ExceptionLogger;
import core.payments.PremiumManager;
import mysql.modules.patreon.DBPatreon;
import mysql.modules.premium.DBPremium;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "PREMIUM_MODIFY")
public class OnPremiumModify implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        boolean success = false;
        long userId = jsonObject.getLong("user_id");
        int i = jsonObject.getInt("slot");
        long guildId = jsonObject.getLong("guild_id");

        if (DBPremium.canModify(userId, i)) {
            success = true;
            if (guildId != 0) {
                DBPremium.modify(userId, i, guildId);
            } else {
                DBPremium.delete(userId, i);
            }
            DBPatreon.transferToNewSystem(userId);
            broadcastPatreonData();
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("success", success);
        return jsonResponse;
    }

    private void broadcastPatreonData() {
        JSONObject jsonObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getClusters()
                .forEach(c -> c.send(EventOut.PATREON, jsonObject).exceptionally(ExceptionLogger.get()));
    }

}