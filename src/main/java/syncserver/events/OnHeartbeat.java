package syncserver.events;

import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "HEARTBEAT")
public class OnHeartbeat implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        String ip = jsonObject.getString("ip");
        boolean alreadyConnected = jsonObject.getBoolean("already_connected");
        if (ClusterConnectionManager.heartbeat(clusterId)) {
            if (alreadyConnected) {
                ClusterConnectionManager.registerConnectedCluster(
                        clusterId,
                        jsonObject.getInt("shard_min"),
                        jsonObject.getInt("shard_max"),
                        jsonObject.getInt("total_shards"),
                        ip
                );
            } else {
                ClusterConnectionManager.registerUnconnectedCluster(clusterId, ip);
            }
        } else {
            if (alreadyConnected) {
                ClusterConnectionManager.clusterSetFullyConnected(
                        clusterId,
                        jsonObject.getInt("shard_min"),
                        jsonObject.getInt("shard_max"),
                        jsonObject.getInt("total_shards"),
                        ip
                );
            }
        }
        return null;
    }

}