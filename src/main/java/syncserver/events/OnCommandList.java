package syncserver.events;

import java.util.Optional;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "COMMAND_LIST")
public class OnCommandList implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        Optional<Cluster> clusterOpt = ClusterConnectionManager.getFirstFullyConnectedCluster();
        return clusterOpt.map(cluster -> SendEvent.sendEmpty("COMMAND_LIST", cluster.getClusterId()).join()).orElse(null);
    }

}