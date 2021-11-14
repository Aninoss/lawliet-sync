package syncserver.events;

import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "PING")
public class OnPing implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        SyncManager.getInstance().getServer().send(socketId, "PING", new JSONObject());
        return null;
    }

}