package syncserver.events;

import java.time.Instant;
import mysql.RedisManager;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "REPORT")
public class OnReport implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        String url = jsonObject.getString("url");
        String text = jsonObject.getString("text");
        int ipHash = jsonObject.getInt("ip_hash");
        RedisManager.update(jedis -> {
            if (!jedis.hexists("reports", url) &&
                    !jedis.hexists("reports_whitelist", url) &&
                    !jedis.hexists("reports_banned", String.valueOf(ipHash))
            ) {
                Cluster cluster = ClusterConnectionManager.getResponsibleCluster(557953262305804308L);
                JSONObject dataJson = new JSONObject();
                dataJson.put("url", url);
                dataJson.put("text", text);
                dataJson.put("ip_hash", ipHash);
                cluster.send(EventOut.REPORT, dataJson).join();
                jedis.hset("reports", url, Instant.now().toString());
            }
        });
        return new JSONObject();
    }

}
