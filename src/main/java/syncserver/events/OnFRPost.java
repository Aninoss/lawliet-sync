package syncserver.events;

import core.ExceptionLogger;
import mysql.modules.featurerequests.DBFeatureRequests;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "FR_POST")
public class OnFRPost implements SyncServerFunction {

    private int counter = -1;

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        String title = jsonObject.getString("title");
        String desc = jsonObject.getString("description");
        int id = getNextId();

        DBFeatureRequests.postFeatureRequest(id, userId, title, desc);
        ClusterConnectionManager.getFirstFullyConnectedCluster().ifPresent(cluster -> {
            SyncUtil.sendUserNotification(
                    cluster,
                    ClusterConnectionManager.OWNER_ID,
                    title == null || title.isEmpty() ? "Empty Title" : title,
                    desc,
                    "Feature Request: " + id,
                    null,
                    null,
                    String.valueOf(userId)
            ).exceptionally(ExceptionLogger.get());
        });

        return new JSONObject();
    }

    private int getNextId() {
        if (counter < 0) {
            counter = DBFeatureRequests.getNewestId();
        }
        return ++counter;
    }

}
