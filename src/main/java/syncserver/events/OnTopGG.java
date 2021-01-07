package syncserver.events;

import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            ClusterConnectionManager.getInstance().getActiveClusters()
                    .forEach(c -> SendEvent.sendJSONSecure("TOPGG", c.getClusterId(), jsonObject).exceptionally(ExceptionLogger.get()));
        }
        return null;
    }

}