package syncserver.events;

import java.util.Optional;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "FAQ_LIST")
public class OnFAQList implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            Optional<Cluster> clusterOpt = ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster();
            return clusterOpt.map(cluster -> SendEvent.sendEmpty("FAQ_LIST", cluster.getClusterId()).join()).orElse(null);
        }
        return null;
    }

}