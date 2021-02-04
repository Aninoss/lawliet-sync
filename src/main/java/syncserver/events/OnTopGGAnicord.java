package syncserver.events;

import core.ExceptionLogger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGGAnicord.class);

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.equals(ClientTypes.WEB)) {
            LOGGER.info("UPVOTE ANINOSS | {}", dataJson.getLong("user"));
            long serverId = dataJson.getLong("guild");
            Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(serverId);
            SendEvent.sendJSONSecure("TOPGG_ANICORD", cluster.getClusterId(), dataJson)
                    .exceptionally(ExceptionLogger.get());
        }
        return null;
    }

}