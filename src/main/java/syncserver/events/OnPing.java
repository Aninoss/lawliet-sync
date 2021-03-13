package syncserver.events;

import org.json.JSONObject;
import syncserver.SendEvent;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PING")
public class OnPing implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        int clusterId = Integer.parseInt(socketId.split("_")[1]);
        SendEvent.sendEmpty("PING", clusterId);
        return null;
    }

}