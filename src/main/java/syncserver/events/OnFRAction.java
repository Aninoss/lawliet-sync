package syncserver.events;

import core.FeatureRequests;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FR_ACTION")
public class OnFRAction implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        int id = jsonObject.getInt("id");
        boolean accept = jsonObject.getBoolean("accept");
        String reason = jsonObject.getString("reason");

        if (accept) {
            FeatureRequests.accept(id);
        } else {
            FeatureRequests.deny(id, reason);
        }

        return null;
    }

}
