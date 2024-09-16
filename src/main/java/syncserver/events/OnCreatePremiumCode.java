package syncserver.events;

import core.util.StringUtil;
import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "CREATE_PREMIUM_CODE")
public class OnCreatePremiumCode implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        PremiumCodeEntity.Level level = PremiumCodeEntity.Level.valueOf(jsonObject.getString("level"));
        int days = jsonObject.getInt("days");
        int quantity = jsonObject.getInt("quantity");

        HibernateManager.run(Database.WEB, entityManager -> {
            entityManager.getTransaction().begin();
            for (int i = 0; i < quantity; i++) {
                PremiumCodeEntity premiumCodeEntity = new PremiumCodeEntity(StringUtil.generateRandomString(20), level, days, userId);
                entityManager.persist(premiumCodeEntity);
            }
            entityManager.getTransaction().commit();
        });

        JSONObject responseJson = new JSONObject();
        responseJson.put("ok", true);
        return responseJson;
    }

}
