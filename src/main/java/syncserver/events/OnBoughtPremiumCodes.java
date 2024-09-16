package syncserver.events;

import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.util.Comparator;

@SyncServerEvent(event = "BOUGHT_PREMIUM_CODES")
public class OnBoughtPremiumCodes implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");

        JSONObject responseJson = new JSONObject();
        JSONArray codesJson = new JSONArray();

        HibernateManager.run(Database.WEB, entityManager -> {
            PremiumCodeEntity.findAllBoughtByUserId(entityManager, userId).stream()
                    .filter(c -> !c.isRedeemed())
                    .sorted(Comparator.comparing(PremiumCodeEntity::getCreatedTime))
                    .map(PremiumCodeEntity::getCode)
                    .forEach(codesJson::put);
        });

        responseJson.put("codes", codesJson);
        return responseJson;
    }

}
