package syncserver.events;

import syncserver.ClusterConnectionManager;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "CLUSTER_FULLY_CONNECTED")
public class OnClusterFullyConnected implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        int clusterId = Integer.parseInt(socketId);
        ClusterConnectionManager.getInstance().connected(clusterId);
        return null;
    }

}