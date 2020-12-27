package syncserver.events;

import org.json.JSONObject;
import syncserver.*;

import java.util.Optional;

@SyncServerEvent(event = "FAQ_LIST")
public class OnFAQList implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        Optional<Cluster> clusterOpt = ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster();
        return clusterOpt.map(cluster -> SendEvent.sendEmpty("FAQ_LIST", cluster.getClusterId()).join()).orElse(null);
    }

}