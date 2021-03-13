package syncserver.events;

import org.json.JSONObject;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "GLOBAL_SERVER_SIZE")
public class OnGlobalServerSize implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        int clusterId = Integer.parseInt(socketId.split("_")[1]);
        long localServerSize = jsonObject.getLong("local_server_size");
        long globalServerSize = 0;
        JSONObject responseJson = new JSONObject();

        ClusterConnectionManager.getInstance().getCluster(clusterId).setLocalServerSize(localServerSize);
        for (Cluster cluster : ClusterConnectionManager.getInstance().getClusters()) {
            if (cluster.isActive() || cluster.getLocalServerSize().isPresent()) {
                if (cluster.getLocalServerSize().isEmpty()) {
                    globalServerSize = 0;
                    break;
                }
                globalServerSize += cluster.getLocalServerSize().get();
            }
        }

        responseJson.put("size", globalServerSize);
        return responseJson;
    }

}