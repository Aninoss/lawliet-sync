package syncserver.events;

import core.cache.PatreonCache;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;
import java.util.HashMap;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            JSONObject responseJson = new JSONObject();
            JSONArray usersArray = new JSONArray();

            HashMap<Long, Integer> userTiersMap = PatreonCache.getInstance().getUserTiersMap();
            userTiersMap.forEach((userId, tier) -> {
                JSONObject userJson = new JSONObject();
                userJson.put("user_id", userId);
                userJson.put("tier", tier);
                usersArray.put(userJson);
            });

            responseJson.put("users", usersArray);
            return responseJson;
        }
        return null;
    }

}