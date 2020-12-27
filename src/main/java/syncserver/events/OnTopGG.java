package syncserver.events;

import org.javacord.api.util.logging.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

import java.util.Optional;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        ClusterConnectionManager.getInstance().getActiveClusters()
                .forEach(c -> SendEvent.sendJSON("TOPGG", c.getClusterId(), jsonObject).exceptionally(ExceptionLogger.get()));
        return null;
    }

}