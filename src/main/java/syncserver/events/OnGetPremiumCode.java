package syncserver.events;

import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "GET_PREMIUM_CODE")
public class OnGetPremiumCode implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        String code = jsonObject.getString("code");

        JSONObject responseJson = new JSONObject();
        HibernateManager.run(Database.WEB, entityManager -> {
            PremiumCodeEntity premiumCodeEntity = entityManager.find(PremiumCodeEntity.class, code);
            if (premiumCodeEntity != null && !premiumCodeEntity.isRedeemed()) {
                responseJson.put("level", premiumCodeEntity.getLevel().name());
                responseJson.put("durationDays", premiumCodeEntity.getDurationDays());
                responseJson.put("found", true);
            }
        });
        return responseJson;
    }

}
