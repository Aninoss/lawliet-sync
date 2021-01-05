package syncserver.events;

import core.SyncedRatelimitManager;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SYNCED_RATELIMIT")
public class OnSyncedRatelimit implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(String s, JSONObject jsonObject) {
        JSONObject responseJson = new JSONObject();
        responseJson.put("waiting_time_nanos", SyncedRatelimitManager.getInstance().processWaitingTimeNanos());
        return responseJson;
    }

}
