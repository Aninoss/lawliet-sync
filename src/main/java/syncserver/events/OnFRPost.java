package syncserver.events;

import mysql.modules.featurerequests.DBFeatureRequests;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FR_POST")
public class OnFRPost implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject requestJSON) {
        long userId = requestJSON.getLong("user_id");
        String title = requestJSON.getString("title");
        String desc = requestJSON.getString("description");
        boolean notify = requestJSON.getBoolean("notify");

        DBFeatureRequests.postFeatureRequest(userId, title, desc);

        //TODO Notify Owner
        return new JSONObject();
    }

}
