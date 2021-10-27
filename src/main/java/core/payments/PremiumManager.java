package core.payments;

import java.util.*;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import mysql.modules.patreon.DBPatreon;
import mysql.modules.patreon.PatreonBean;
import mysql.modules.premium.DBPremium;
import mysql.modules.premium.PremiumSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PremiumManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(PremiumManager.class);

    public static int retrieveBoostsTotal(long userId) {
        int patreonBoosts = PatreonCache.getInstance().getUserTier(userId) - 1;
        int stripeBoosts = 0;
        try {
            if (StripeManager.retrieveActiveSubscriptions(userId).size() > 0) {
                stripeBoosts = 2;
            }
        } catch (StripeException e) {
            LOGGER.error("Stripe exception", e);
        }
        return 1 + Math.max(patreonBoosts, stripeBoosts);
    }

    public static int retrieveUnlockServersNumber(long userId) {
        int n = tierToPremiumSlotNumber(PatreonCache.getInstance().getUserTier(userId));
        try {
            for (Subscription subscription : StripeManager.retrieveActiveSubscriptions(userId)) {
                Map<String, String> metadata = subscription.getMetadata();
                if (subscription.getMetadata().containsKey("unlock_servers") && Boolean.parseBoolean(metadata.get("unlock_servers"))) {
                    n += subscription.getItems().getData().get(0).getQuantity();
                }
            }
        } catch (StripeException e) {
            LOGGER.error("Stripe exception", e);
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
        Map<Long, ArrayList<Subscription>> stripeMap = null;
        try {
            stripeMap = StripeManager.retrieveActiveSubscriptions();
            for (long userId : stripeMap.keySet()) {
                patreonTiers.putIfAbsent(userId, 2);
            }
        } catch (StripeException e) {
            LOGGER.error("Stripe exception", e);
        }

        Map<Long, ArrayList<Subscription>> finalStripeMap = stripeMap;
        patreonTiers.forEach((userId, tier) -> {
            JSONObject userJson = new JSONObject();
            userJson.put("user_id", userId);
            userJson.put("tier", tier);
            usersArray.put(userJson);

            int slots = tierToPremiumSlotNumber(tier);
            if (finalStripeMap != null && finalStripeMap.containsKey(userId)) {
                slots += finalStripeMap.get(userId).stream()
                        .filter(s -> s.getMetadata().containsKey("unlock_servers") && Boolean.parseBoolean(s.getMetadata().get("unlock_servers")))
                        .mapToInt(s -> Math.toIntExact(s.getItems().getData().get(0).getQuantity()))
                        .sum();
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
