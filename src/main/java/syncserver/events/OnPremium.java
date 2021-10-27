package syncserver.events;

import java.util.HashMap;
import core.payments.PremiumManager;
import mysql.modules.premium.DBPremium;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PREMIUM")
public class OnPremium implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            JSONObject jsonResponse = new JSONObject();
            JSONArray jsonSlots = new JSONArray();
            JSONArray jsonGuilds = new JSONArray();

            long userId = jsonObject.getLong("user_id");
            int n = PremiumManager.retrieveUnlockServersNumber(userId);

            HashMap<Integer, Long> slotMap = DBPremium.fetchForUser(userId);
            for (int i = 0; i < n; i++) {
                jsonSlots.put(getSlot(slotMap, i));
            }

            jsonResponse.put("guilds", jsonGuilds);
            jsonResponse.put("slots", jsonSlots);

            return jsonResponse;
        }
        return null;
    }

    private long getSlot(HashMap<Integer, Long> slotMap, int i) {
        if (slotMap.containsKey(i)) {
            return slotMap.get(i);
        }
        return 0;
    }

}