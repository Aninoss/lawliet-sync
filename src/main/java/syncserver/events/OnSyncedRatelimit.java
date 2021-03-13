package syncserver.events;

import core.ExceptionLogger;
import core.SyncedRatelimitManager;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "SYNCED_RATELIMIT")
public class OnSyncedRatelimit implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            int clusterId = Integer.parseInt(socketId.split("_")[1]);
            ClusterConnectionManager.getInstance().getActiveClusters().stream()
                    .filter(c -> c.getClusterId() != clusterId)
                    .forEach(c -> SendEvent.sendRequestSyncedRatelimit(c.getClusterId(), jsonObject.getLong("ratelimit"))
                            .exceptionally(ExceptionLogger.get())
                    );
        }
        return null;
    }

}
