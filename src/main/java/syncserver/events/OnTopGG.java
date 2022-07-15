package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGG.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("success", false);
        LOGGER.info("UPVOTE | {}", jsonObject.getLong("user"));
        try {
            ClusterConnectionManager.getCluster(1)
                    .send("TOPGG", jsonObject).get(5, TimeUnit.SECONDS);
            responseJson.put("success", true);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error", e);
        }
        return responseJson;
    }

}