package syncserver.events;

import mysql.modules.premium.DBPremium;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PREMIUM_MODIFY")
public class OnPremiumModify implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
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
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("success", success);
            return jsonResponse;
        }
        return null;
    }

}