package syncserver.events;

import mysql.RedisManager;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "EXCEPTIONS_PAGE_INIT")
public class OnExceptionsPageInit implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        String dir = jsonObject.getString("dir");

        JSONObject responseJson = new JSONObject();
        responseJson.put("hide", RedisManager.get(jedis -> jedis.get("exceptions:" + userId + ":" + dir + ":hide"), ""));
        responseJson.put("group", RedisManager.get(jedis -> jedis.get("exceptions:" + userId + ":" + dir + ":group"), ""));
        return responseJson;
    }

}
