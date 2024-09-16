package syncserver.events;

import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "REDEEMED_PREMIUM_CODES")
public class OnRedeemedPremiumCodes implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");

        JSONObject responseJson = new JSONObject();
        JSONArray codesJson = new JSONArray();

        HibernateManager.run(Database.WEB, entityManager -> {
            PremiumCodeEntity.findAllRedeemedByUserId(entityManager, userId)
                    .forEach(code -> {
                        JSONObject codeJson = new JSONObject();
                        codeJson.put("code", code.getCode());
                        codeJson.put("level", code.getLevel().name());
                        codeJson.put("durationDays", code.getDurationDays());
                        codeJson.put("expiration", code.getExpiration().toString());
                        codesJson.put(codeJson);
                    });
        });

        responseJson.put("codes", codesJson);
        return responseJson;
    }

}
