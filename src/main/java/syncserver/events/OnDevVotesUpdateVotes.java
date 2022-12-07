package syncserver.events;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import mysql.modules.devvotes.DBDevVotes;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "DEV_VOTES_UPDATE_VOTES")
public class OnDevVotesUpdateVotes implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) >= 8) {
            return null;
        }

        long userId = jsonObject.getLong("user_id");
        int year = jsonObject.getInt("year");
        int month = jsonObject.getInt("month");

        ArrayList<String> votes = new ArrayList<>();
        JSONArray votesJsonArray = jsonObject.getJSONArray("votes");
        for (int i = 0; i < votesJsonArray.length(); i++) {
            votes.add(votesJsonArray.getString(i));
        }

        try {
            DBDevVotes.updateVotes(userId, year, month, votes);
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
