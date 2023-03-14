package syncserver.events;

import core.ExceptionLogger;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "CMD")
public class OnCmd implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        int toClusterId = jsonObject.getInt("cluster_id");
        String command = jsonObject.getString("command");
        if (toClusterId != -1) {
            SyncUtil.sendCmd(ClusterConnectionManager.getCluster(toClusterId), command)
                    .exceptionally(ExceptionLogger.get());
        } else {
            ClusterConnectionManager.getClusters()
                    .forEach(c -> SyncUtil.sendCmd(c, command).exceptionally(ExceptionLogger.get()));
        }
        return null;
    }

}