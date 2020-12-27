package syncserver.events;

import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        long serverId = dataJson.getLong("server_id");
        Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(serverId);
        SendEvent.sendJSON("TOPGG", cluster.getClusterId(), dataJson).exceptionally(ExceptionLogger.get());
        return null;
    }

}