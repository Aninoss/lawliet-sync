package syncserver.events;

import java.util.Optional;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "FAQ_LIST")
public class OnFAQList implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        Optional<Cluster> clusterOpt = ClusterConnectionManager.getFirstFullyConnectedCluster();
        return clusterOpt.map(cluster -> cluster.send(EventOut.FAQ_LIST).join()).orElse(null);
    }

}