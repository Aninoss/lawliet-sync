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

@SyncServerEvent(event = "FR_CAN_POST")
public class OnFRCanPost implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            long userId = requestJSON.getLong("user_id");
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("success", DBFeatureRequests.canPost(userId));

            return responseJSON;
        }
        return null;
    }

}
