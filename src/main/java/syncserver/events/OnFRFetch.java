package syncserver.events;

import core.cache.PatreonCache;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.featurerequests.DBFeatureRequests;
import mysql.modules.featurerequests.FRPanelType;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FR_FETCH")
public class OnFRFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("boosts_total", 0);
            responseJSON.put("boosts_remaining", 0);

            long userId = -1;
            if (requestJSON.has("user_id")) {
                userId = requestJSON.getLong("user_id");

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

            responseJSON.put("data", jsonEntriesArray);
            return responseJSON;
        }
        return null;
    }

    public static int getBoostsTotal(long userId) {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId)) {
            return 0;
        }

        return Math.max(1, PatreonCache.getInstance().getUserTier(userId));
    }

    public static int getBoostsUsed(long userId) {
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId)) {
            return 0;
        }

        return DBFeatureRequests.fetchBoostsThisWeek(userId);
    }

}
