package syncserver.events;

import core.payments.PremiumManager;
import mysql.modules.devvotes.DBDevVotes;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "DEV_VOTES_INIT")
public class OnDevVotesInit implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        int year = jsonObject.getInt("year");
        int month = jsonObject.getInt("month");
        boolean premium = PremiumManager.userIsPremium(userId);

        JSONObject responseJson = new JSONObject();
        responseJson.put("premium", premium);

        if (premium) {
            responseJson.put("reminder_active", DBDevVotes.reminderIsActive(userId));
            JSONArray votesJsonArray = new JSONArray();
            for (String userVote : DBDevVotes.getUserVotes(userId, year, month)) {
                votesJsonArray.put(userVote);
            }
            responseJson.put("votes", votesJsonArray);
        }

        return responseJson;
    }

}
