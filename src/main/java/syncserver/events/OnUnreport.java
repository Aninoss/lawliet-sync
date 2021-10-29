package syncserver.events;

import java.time.Instant;
import mysql.RedisManager;
import org.json.JSONObject;
import redis.clients.jedis.Pipeline;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "UNREPORT")
public class OnUnreport implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            String url = requestJSON.getString("url");
            RedisManager.update(jedis -> {
                Pipeline pipeline = jedis.pipelined();
                pipeline.hdel("reports", url);
                pipeline.hset("reports_whitelist", url, Instant.now().toString());
                pipeline.sync();
            });
            return new JSONObject();
        }
        return null;
    }

}
