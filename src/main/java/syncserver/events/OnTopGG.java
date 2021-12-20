package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.ClientTypes;
import syncserver.SendEvent;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGG.class);

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("success", false);
        if (socketId.equals(ClientTypes.WEB)) {
            LOGGER.info("UPVOTE | {}", jsonObject.getLong("user"));
            try {
                SendEvent.sendJSON("TOPGG", 1, jsonObject).get(5, TimeUnit.SECONDS);
                responseJson.put("success", true);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Error", e);
            }
        }
        return responseJson;
    }

}