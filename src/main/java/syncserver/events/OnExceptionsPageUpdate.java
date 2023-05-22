package syncserver.events;

import mysql.RedisManager;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "EXCEPTIONS_PAGE_UPDATE")
public class OnExceptionsPageUpdate implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        String dir = jsonObject.getString("dir");
        String hide = jsonObject.getString("hide");
        String group = jsonObject.getString("group");

        RedisManager.update(jedis -> {
            jedis.set("exceptions:" + userId + ":" + dir + ":hide", hide);
            jedis.set("exceptions:" + userId + ":" + dir + ":group", group);
        });

        return null;
    }

}
