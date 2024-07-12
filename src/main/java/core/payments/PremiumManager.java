package core.payments;

import com.stripe.model.Subscription;
import core.Program;
import core.payments.paddle.PaddleManager;
import core.payments.paddle.PaddleSubscription;
import core.payments.stripe.StripeManager;
import hibernate.HibernateManager;
import hibernate.entities.PremiumCodeEntity;
import mysql.modules.patreon.DBPatreon;
import mysql.modules.patreon.PatreonBean;
import mysql.modules.premium.DBPremium;
import mysql.modules.premium.PremiumSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

public class PremiumManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(PremiumManager.class);

    public static boolean userIsPremium(long userId) {
        if (!Program.isProductionMode() ||
                PatreonCache.getInstance().getUserTier(userId) > 0 ||
                !StripeManager.retrieveActiveSubscriptions(userId).isEmpty() ||
                !PaddleManager.retrieveActiveSubscriptionsByUserId(userId).isEmpty()
        ) {
            return true;
        }

        EntityManager entityManager = HibernateManager.creasteEntityManager();
        try {
            return !PremiumCodeEntity.findAllActiveRedeemedByUserId(entityManager, userId).isEmpty();
        } finally {
            entityManager.close();
        }
    }

    public static int retrieveBoostsTotal(long userId) {
        int patreonBoosts = PatreonCache.getInstance().getUserTier(userId) - 1;
        int stripeBoosts = 0;
        if (!StripeManager.retrieveActiveSubscriptions(userId).isEmpty()) {
            stripeBoosts = 2;
        } else if (!PaddleManager.retrieveActiveSubscriptionsByUserId(userId).isEmpty()) {
            stripeBoosts = 2;
        } else {
            EntityManager entityManager = HibernateManager.creasteEntityManager();
            try {
                if (!PremiumCodeEntity.findAllActiveRedeemedByUserId(entityManager, userId).isEmpty()) {
                    stripeBoosts = 2;
                }
            } finally {
                entityManager.close();
            }
        }
        return 1 + Math.max(patreonBoosts, stripeBoosts);
    }

    public static int retrieveUnlockServersNumber(long userId) {
        int n = tierToPremiumSlotNumber(PatreonCache.getInstance().getUserTier(userId));

        for (Subscription subscription : StripeManager.retrieveActiveSubscriptions(userId)) {
            Map<String, String> metadata = subscription.getMetadata();
            if (subscription.getMetadata().containsKey("unlock_servers") && Boolean.parseBoolean(metadata.get("unlock_servers"))) {
                n += subscription.getItems().getData().get(0).getQuantity();
            }
        }

        for (PaddleSubscription subscription : PaddleManager.retrieveActiveSubscriptionsByUserId(userId)) {
            if (subscription.unlocksServer()) {
                n += subscription.getQuantity();
            }
        }

        EntityManager entityManager = HibernateManager.creasteEntityManager();
        try {
            for (PremiumCodeEntity premiumCodeEntity : PremiumCodeEntity.findAllActiveRedeemedByUserId(entityManager, userId)) {
                if (premiumCodeEntity.getLevel() == PremiumCodeEntity.Level.PRO) {
                    n++;
                }
            }
        } finally {
            entityManager.close();
        }

        return n;
    }

    public static JSONObject retrieveJsonData() {
        LinkedList<Long> unlockedGuilds = new LinkedList<>();
        JSONObject responseJson = new JSONObject();
        JSONArray usersArray = new JSONArray();
        JSONArray oldUsersArray = new JSONArray();
        JSONArray guildsArray = new JSONArray();
        HashMap<Long, ArrayList<PremiumSlot>> userSlotMap = DBPremium.fetchAll();
        HashMap<Long, Integer> patreonTiers = insertDatabasePatreons(PatreonCache.getInstance().getAsync());

        Map<Long, ArrayList<Subscription>> stripeMap = StripeManager.retrieveActiveSubscriptions();
        for (long userId : stripeMap.keySet()) {
            patreonTiers.putIfAbsent(userId, 2);
        }

        Map<Long, ArrayList<PaddleSubscription>> paddleMap = PaddleManager.retrieveActiveSubscriptions();
        for (long userId : paddleMap.keySet()) {
            patreonTiers.putIfAbsent(userId, 2);
        }

        Map<Long, List<PremiumCodeEntity>> codesMap;
        EntityManager entityManager = HibernateManager.creasteEntityManager();
        try {
            List<PremiumCodeEntity> premiumCodeEntities = PremiumCodeEntity.findAllActive(entityManager);
            codesMap = premiumCodeEntities.stream()
                    .collect(Collectors.groupingBy(PremiumCodeEntity::getRedeemedByUserId));
            for (PremiumCodeEntity premiumCodeEntity : premiumCodeEntities) {
                patreonTiers.putIfAbsent(premiumCodeEntity.getRedeemedByUserId(), 2);
            }
        } finally {
            entityManager.close();
        }

        patreonTiers.forEach((userId, tier) -> {
            if (tier == null) {
                LOGGER.warn("Skipping user id {} due to empty tier", userId);
                return;
            }

            JSONObject userJson = new JSONObject();
            userJson.put("user_id", userId);
            userJson.put("tier", tier);
            usersArray.put(userJson);

            int slots = tierToPremiumSlotNumber(tier);
            if (stripeMap.containsKey(userId)) {
                slots += stripeMap.get(userId).stream()
                        .filter(s -> s.getMetadata().containsKey("unlock_servers") && Boolean.parseBoolean(s.getMetadata().get("unlock_servers")))
                        .mapToInt(s -> Math.toIntExact(s.getItems().getData().get(0).getQuantity()))
                        .sum();
            }
            if (paddleMap.containsKey(userId)) {
                slots += paddleMap.get(userId).stream()
                        .filter(PaddleSubscription::unlocksServer)
                        .mapToInt(PaddleSubscription::getQuantity)
                        .sum();
            }
            if (codesMap.containsKey(userId)) {
                slots += (int) codesMap.get(userId).stream()
                        .filter(code -> code.getLevel() == PremiumCodeEntity.Level.PRO)
                        .count();
            }
            ArrayList<PremiumSlot> slotList = userSlotMap.get(userId);
            if (slotList != null) {
                int finalSlots = slots;
                slotList.stream()
                        .filter(premiumSlot -> premiumSlot.getSlot() < finalSlots && !unlockedGuilds.contains(premiumSlot.getGuildId()))
                        .forEach(premiumSlot -> {
                            unlockedGuilds.add(premiumSlot.getGuildId());
                            guildsArray.put(premiumSlot.getGuildId());
                        });
            }
        });

        DBPatreon.retrieveOldUsers().forEach(oldUsersArray::put);

        responseJson.put("users", usersArray);
        responseJson.put("old_users", oldUsersArray);
        responseJson.put("guilds", guildsArray);
        return responseJson;
    }

    private static HashMap<Long, Integer> insertDatabasePatreons(HashMap<Long, Integer> userTiersMap) {
        if (userTiersMap == null) {
            userTiersMap = new HashMap<>();
        }

        HashMap<Long, PatreonBean> sqlMap = DBPatreon.getInstance().getBean();
        HashMap<Long, Integer> finalUserTiersMap = userTiersMap;
        sqlMap.keySet().forEach(userId -> {
            PatreonBean p = sqlMap.get(userId);
            if (p.isValid()) {
                finalUserTiersMap.put(userId, p.getTier());
            }
        });
        return userTiersMap;
    }

    private static int tierToPremiumSlotNumber(int patreonTier) {
        return switch (patreonTier) {
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 5;
            case 6 -> 10;
            default -> 0;
        };
    }

}
