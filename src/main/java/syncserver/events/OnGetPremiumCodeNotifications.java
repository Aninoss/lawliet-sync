package syncserver.events;

import hibernate.Database;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.time.Duration;
import java.time.Instant;

@SyncServerEvent(event = "GET_PREMIUM_CODE_NOTIFICATIONS")
public class OnGetPremiumCodeNotifications implements SyncServerFunction {

    @Override
    public synchronized JSONObject apply(int clusterId, JSONObject jsonObject) {
        JSONArray notificationsJsonArray = new JSONArray();

        HibernateManager.run(Database.WEB, entityManager -> {
            entityManager.getTransaction().begin();
            PremiumCodeEntity.findAllActive(entityManager).stream()
                    .filter(premiumCode -> !premiumCode.getNotificationSent() && Instant.now().isAfter(premiumCode.getExpiration().minus(Duration.ofDays(1))))
                    .forEach(premiumCode -> {
                        JSONObject entryJson = new JSONObject();
                        entryJson.put("user_id", premiumCode.getRedeemedByUserId());
                        entryJson.put("expiration", premiumCode.getExpiration().toString());
                        notificationsJsonArray.put(entryJson);

                        premiumCode.setNotificationSent(true);
                    });
            entityManager.getTransaction().commit();
        });

        JSONObject responseJson = new JSONObject();
        responseJson.put("notifications", notificationsJsonArray);
        return responseJson;
    }

}
