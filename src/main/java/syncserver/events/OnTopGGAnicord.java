package syncserver.events;

import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.equals(ClientTypes.WEB)) {
            long serverId = dataJson.getLong("guild");
            Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(serverId);
            SendEvent.sendJSONSecure("TOPGG_ANICORD", cluster.getClusterId(), dataJson).exceptionally(ExceptionLogger.get());
        }
        return null;
    }

}