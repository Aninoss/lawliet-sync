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

        Cluster cluster = ClusterConnectionManager.getResponsibleCluster(557953262305804308L);
        JSONObject dataJson = new JSONObject();
        dataJson.put("title", title);
        dataJson.put("desc", desc);
        dataJson.put("id", id);
        dataJson.put("user_id", userId);
        cluster.send(EventOut.FR_NOTIFICATION, dataJson)
                .exceptionally(ExceptionLogger.get());

        return new JSONObject();
    }

    private int getNextId() {
        if (counter < 0) {
            counter = DBFeatureRequests.getNewestId();
        }
        return ++counter;
    }

}
