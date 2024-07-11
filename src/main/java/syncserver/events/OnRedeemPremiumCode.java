package syncserver.events;

import core.ExceptionLogger;
import core.payments.PremiumManager;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.EventOut;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.time.Duration;
import java.time.Instant;

@SyncServerEvent(event = "REDEEM_PREMIUM_CODE")
public class OnRedeemPremiumCode implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(int clusterId, JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        String code = jsonObject.getString("code");
        int durationDays = jsonObject.getInt("duration_days");

        JSONObject responseJson = new JSONObject();
        HibernateManager.run(entityManager -> {
            PremiumCodeEntity premiumCodeEntity = entityManager.find(PremiumCodeEntity.class, code);
            if (premiumCodeEntity != null && !premiumCodeEntity.isRedeemed()) {
                entityManager.getTransaction().begin();
                premiumCodeEntity.setRedeemedByUserId(userId);
                premiumCodeEntity.setExpiration(Instant.now().plus(Duration.ofDays(durationDays)));
                entityManager.getTransaction().commit();
                responseJson.put("ok", true);
            }
        });

        JSONObject jsonPremiumObject = PremiumManager.retrieveJsonData();
        ClusterConnectionManager.getClusters()
                .forEach(c -> c.send(EventOut.PATREON, jsonPremiumObject).exceptionally(ExceptionLogger.get()));

        return responseJson;
    }

}
