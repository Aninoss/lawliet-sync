package syncserver.events;

import core.SyncedRatelimitManager;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SYNCED_RATELIMIT")
public class OnSyncedRatelimit implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            JSONObject responseJson = new JSONObject();
            responseJson.put("waiting_time_nanos", SyncedRatelimitManager.getInstance().processWaitingTimeNanos());
            return responseJson;
        }
        return null;
    }

}
