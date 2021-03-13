package syncserver.events;

import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "CLUSTER_FULLY_CONNECTED")
public class OnClusterFullyConnected implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        int clusterId = Integer.parseInt(socketId.split("_")[1]);
        ClusterConnectionManager.getInstance().connected(clusterId);
        return null;
    }

}