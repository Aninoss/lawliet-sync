package syncserver.events;

import core.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "CMD")
public class OnCmd implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.CONSOLE)) {
            int clusterId = jsonObject.getInt("cluster_id");
            String command = jsonObject.getString("command");
            if (clusterId >= 1) {
                SendEvent.sendCmd(clusterId, command)
                        .exceptionally(ExceptionLogger.get());
            } else {
                ClusterConnectionManager.getInstance().getActiveClusters()
                        .forEach(c -> SendEvent.sendCmd(c.getClusterId(), command).exceptionally(ExceptionLogger.get()));
            }
        }
        return null;
    }

}