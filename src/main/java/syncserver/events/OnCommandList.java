package syncserver.events;

import org.javacord.api.entity.webhook.WebhookBuilder;
import org.json.JSONObject;
import syncserver.*;

import java.util.Optional;

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