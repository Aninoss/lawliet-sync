package syncserver.events;

import core.ExceptionLogger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGG.class);

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            LOGGER.info("UPVOTE | {} START", jsonObject.getLong("user"));
            ClusterConnectionManager.getInstance().getActiveClusters()
                    .forEach(c -> {
                        SendEvent.sendJSONSecure("TOPGG", c.getClusterId(), jsonObject)
                                .exceptionally(ExceptionLogger.get());
                    });
        }
        return null;
    }

}