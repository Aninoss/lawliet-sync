package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGGAnicord.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("success", false);
        LOGGER.info("UPVOTE ANINOSS | {}", jsonObject.getLong("user"));
        long guildId = jsonObject.getLong("guild");
        Cluster cluster = ClusterConnectionManager.getResponsibleCluster(guildId);
        try {
            SendEvent.sendJSON("TOPGG_ANICORD", cluster.getClusterId(), jsonObject).get(5, TimeUnit.SECONDS);
            responseJson.put("success", true);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error", e);
        }
        return responseJson;
    }

}