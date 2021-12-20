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
    public JSONObject apply(String socketId, JSONObject dataJson) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("success", false);
        if (socketId.equals(ClientTypes.WEB)) {
            LOGGER.info("UPVOTE ANINOSS | {}", dataJson.getLong("user"));
            long guildId = dataJson.getLong("guild");
            Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(guildId);
            try {
                SendEvent.sendJSON("TOPGG_ANICORD", cluster.getClusterId(), dataJson).get(5, TimeUnit.SECONDS);
                responseJson.put("success", true);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Error", e);
            }
        }
        return responseJson;
    }

}