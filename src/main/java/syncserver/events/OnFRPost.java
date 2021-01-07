package syncserver.events;

import mysql.modules.featurerequests.DBFeatureRequests;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "FR_POST")
public class OnFRPost implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        if (socketId.equals(ClientTypes.WEB)) {
            long userId = requestJSON.getLong("user_id");
            String title = requestJSON.getString("title");
            String desc = requestJSON.getString("description");
            boolean notify = requestJSON.getBoolean("notify");

            DBFeatureRequests.postFeatureRequest(userId, title, desc);
            ClusterConnectionManager.getInstance().getFirstFullyConnectedCluster().ifPresent(cluster -> {
                SendEvent.sendUserNotification(
                        cluster.getClusterId(),
                        ClusterConnectionManager.OWNER_ID,
                        title,
                        desc,
                        "FEATURE REQUEST",
                        null,
                        null,
                        notify ? String.valueOf(userId) : null
                );
            });

            return new JSONObject();
        }
        return null;
    }

}
