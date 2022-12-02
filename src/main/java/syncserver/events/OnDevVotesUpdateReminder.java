package syncserver.events;

import java.sql.SQLException;
import mysql.modules.devvotes.DBDevVotes;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "DEV_VOTES_UPDATE_REMINDER")
public class OnDevVotesUpdateReminder implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        Boolean active = null;
        if (jsonObject.has("active")) {
            active = jsonObject.getBoolean("active");
        }
        String locale = jsonObject.getString("locale");

        try {
            DBDevVotes.updateReminder(userId, active, locale);
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
