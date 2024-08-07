package syncserver.events;

import core.payments.PremiumManager;
import mysql.modules.featurerequests.DBFeatureRequests;
import mysql.modules.featurerequests.FRPanelType;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FR_FETCH")
public class OnFRFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("boosts_total", 0);
        responseJSON.put("boosts_remaining", 0);

        long userId = -1;
        if (jsonObject.has("user_id")) {
            userId = jsonObject.getLong("user_id");

            int boostsTotal = getBoostsTotal(userId);
            responseJSON.put("boosts_total", boostsTotal);

            int boostsUsed = getBoostsUsed(userId);
            if (boostsUsed != -1) {
                responseJSON.put("boosts_remaining", Math.max(0, boostsTotal - boostsUsed));
            }
        }

        JSONArray jsonEntriesArray = new JSONArray();
        FRPanelType[] types = new FRPanelType[] { FRPanelType.PENDING, FRPanelType.REJECTED };
        for (FRPanelType type : types) {
            DBFeatureRequests.fetchEntries(userId, type).forEach(frEntry -> {
                JSONObject jsonEntry = new JSONObject()
                        .put("id", frEntry.getId())
                        .put("title", frEntry.getTitle())
                        .put("description", frEntry.getDescription())
                        .put("public", frEntry.isPublicEntry())
                        .put("boosts", frEntry.getBoosts())
                        .put("recent_boosts", frEntry.getRecentBoosts())
                        .put("type", type.name())
                        .put("date", frEntry.getDate().toEpochDay());
                jsonEntriesArray.put(jsonEntry);
            });
        }

        responseJSON.put("completed", DBFeatureRequests.fetchEntries(userId, FRPanelType.COMPLETED).size());
        responseJSON.put("data", jsonEntriesArray);
        return responseJSON;
    }

    public static int getBoostsTotal(long userId) {
        return PremiumManager.retrieveBoostsTotal(userId);
    }

    public static int getBoostsUsed(long userId) {
        return DBFeatureRequests.fetchBoostsThisWeek(userId);
    }

}
