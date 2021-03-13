package syncserver.events;

import java.util.Optional;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "COMMAND_LIST")
public class OnCommandList implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            Optional<Cluster> clusterOpt = ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster();
            return clusterOpt.map(cluster -> SendEvent.sendEmpty("COMMAND_LIST", cluster.getClusterId()).join()).orElse(null);
        }
        return null;
    }

}