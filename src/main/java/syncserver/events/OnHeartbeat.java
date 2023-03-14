package syncserver.events;

import java.util.HashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.Cluster;
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
                        jsonObject.getInt("total_shards"),
                        ip
                );
            } else if (ClusterConnectionManager.getCluster(clusterId).getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED) {
                ClusterConnectionManager.registerUnconnectedCluster(clusterId, ip);
            }
        }

        long totalServers;
        if (jsonObject.has("total_servers") && (totalServers = jsonObject.getLong("total_servers")) > 0L) {
            ClusterConnectionManager.getCluster(clusterId).setLocalServerSize(totalServers);
        }

        if (jsonObject.has("server_ids")) {
            HashSet<Long> serverIds = new HashSet<>();
            JSONArray serverIdsJsonArray = jsonObject.getJSONArray("server_ids");
            for (int i = 0; i < serverIdsJsonArray.length(); i++) {
                serverIds.add(serverIdsJsonArray.getLong(i));
            }
            ClusterConnectionManager.getCluster(clusterId).setServerIds(serverIds);
        }

        return null;
    }

}