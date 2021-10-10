package syncserver.events;

import java.time.Instant;
import mysql.RedisManager;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "UNREPORT")
public class OnUnreport implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            String url = requestJSON.getString("url");
            RedisManager.update(jedis -> jedis.hdel("reports", url));
            return new JSONObject();
        }
        return null;
    }

}
