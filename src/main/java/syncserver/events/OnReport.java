package syncserver.events;

import java.time.Instant;
import mysql.RedisManager;
import mysql.modules.featurerequests.DBFeatureRequests;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "REPORT")
public class OnReport implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            String url = requestJSON.getString("url");
            String text = requestJSON.getString("text");
            RedisManager.update(jedis -> {
                String instant = jedis.hget("reports", url);
                if (instant == null) {
                    Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(557953262305804308L);
                    SendEvent.sendReport(cluster.getClusterId(), url, text).join();
                    jedis.hset("reports", url, Instant.now().toString());
                }
            });
            return new JSONObject();
        }
        return null;
    }

}
