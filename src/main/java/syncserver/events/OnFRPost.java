package syncserver.events;

import mysql.modules.featurerequests.DBFeatureRequests;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "FR_POST")
public class OnFRPost implements SyncServerFunction {

    private int counter = -1;

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            long userId = requestJSON.getLong("user_id");
            String title = requestJSON.getString("title");
            String desc = requestJSON.getString("description");
            int id = getNextId();

            DBFeatureRequests.postFeatureRequest(id, userId, title, desc);
            ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster().ifPresent(cluster -> {
                SendEvent.sendUserNotification(
                        cluster.getClusterId(),
                        ClusterConnectionManager.OWNER_ID,
                        title,
                        desc,
                        "Feature Request: " + id,
                        null,
                        null,
                        String.valueOf(userId)
                );
            });

            return new JSONObject();
        }
        return null;
    }

    private int getNextId() {
        if (counter < 0) {
            counter = DBFeatureRequests.getNewestId();
        }
        return ++counter;
    }

}
