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
        boolean active = jsonObject.getBoolean("active");

        try {
            DBDevVotes.updateReminder(userId, active);
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
