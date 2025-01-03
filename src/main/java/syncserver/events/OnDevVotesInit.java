package syncserver.events;

import core.payments.PremiumManager;
import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.DiscordSubscriptionEntity;
import mysql.modules.devvotes.DBDevVotes;
import mysql.modules.devvotes.VoteResultSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Objects;

@SyncServerEvent(event = "DEV_VOTES_INIT")
public class OnDevVotesInit implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        int year = jsonObject.getInt("year");
        int month = jsonObject.getInt("month");

        String locale = jsonObject.getString("locale");
        boolean premium = PremiumManager.userIsPremium(userId) ||
                HibernateManager.apply(Database.BOT, entityManager -> !DiscordSubscriptionEntity.findValidDiscordSubscriptionEntitiesByUserId(entityManager, userId).isEmpty());

        JSONObject responseJson = new JSONObject();
        responseJson.put("premium", premium);

        if (premium) {
            Boolean active = DBDevVotes.reminderIsActive(userId);
            responseJson.put("reminder_active", Objects.requireNonNullElse(active, true));

            if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) < 8 && Calendar.getInstance().get(Calendar.MONTH) != Calendar.JANUARY) {
                JSONArray votesJsonArray = new JSONArray();
                for (String userVote : DBDevVotes.getUserVotes(userId, year, month)) {
                    votesJsonArray.put(userVote);
                }
                responseJson.put("votes", votesJsonArray);
            } else {
                JSONArray voteResultJsonArray = new JSONArray();
                for (VoteResultSlot slot : DBDevVotes.getVoteResult(year, month)) {
                    JSONObject voteResultJson = new JSONObject();
                    voteResultJson.put("id", slot.getId());
                    voteResultJson.put("number", slot.getNumber());
                    voteResultJsonArray.put(voteResultJson);
                }
                responseJson.put("vote_result", voteResultJsonArray);
                responseJson.put("total_votes", DBDevVotes.getVoteResultTotalVotes(year, month));
            }

            try {
                DBDevVotes.updateReminder(userId, active, locale);
            } catch (SQLException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return responseJson;
    }

}
