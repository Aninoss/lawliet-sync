package syncserver.events;

import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SYNCED_RATELIMIT")
public class OnSyncedRatelimit implements SyncServerFunction {

    private final long INTERVAL_TIME_NANOS = 21_000_000;

    private volatile long nextRequest = 0;

    @Override
    public synchronized JSONObject apply(String s, JSONObject jsonObject) {
        long currentTime = System.nanoTime();
        this.nextRequest = Math.max(currentTime + INTERVAL_TIME_NANOS, this.nextRequest + INTERVAL_TIME_NANOS);

        JSONObject responseJson = new JSONObject();
        responseJson.put("waiting_time_nanos", Math.max(0, nextRequest - currentTime));
        return responseJson;
    }

}
