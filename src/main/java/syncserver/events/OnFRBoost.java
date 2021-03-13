package syncserver.events;

import mysql.modules.featurerequests.DBFeatureRequests;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FR_BOOST")
public class OnFRBoost implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            final JSONObject responseJSON = new JSONObject();
            final long userId = requestJSON.getLong("user_id");
            final int entryId = requestJSON.getInt("entry_id");

            int boostsTotal = OnFRFetch.getBoostsTotal(userId);
            int boostsUsed = OnFRFetch.getBoostsUsed(userId);
            int boostRemaining = 0;
            if (boostsUsed != -1)
                boostRemaining = Math.max(0, boostsTotal - boostsUsed);

            boolean success = boostRemaining > 0;
            if (success) {
                DBFeatureRequests.insertBoost(entryId, userId);
                boostRemaining--;
            }

            responseJSON.put("boosts_total", boostsTotal);
            responseJSON.put("boosts_remaining", boostRemaining);
            responseJSON.put("success", success);

            return responseJSON;
        }
        return null;
    }

}
