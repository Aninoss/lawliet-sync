package core.payments.paddlebilling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PaddleBillingManager {

    public static Map<Long, ArrayList<PaddleBillingSubscription>> retrieveActiveSubscriptions() {
        HashMap<Long, ArrayList<PaddleBillingSubscription>> map = new HashMap<>();
        for (PaddleBillingSubscription subscription : retrieveActiveSubscriptionsByUserId(0L)) {
            long userId = subscription.getUserId();
            ArrayList<PaddleBillingSubscription> subscriptions = map.computeIfAbsent(userId, k -> new ArrayList<>());
            subscriptions.add(subscription);
            map.put(userId, subscriptions);
        }
        return map;
    }

    public static List<PaddleBillingSubscription> retrieveActiveSubscriptionsByUserId(long userId) {
        return PaddleBillingCache.getSubscriptions()
                .stream()
                .filter(sub -> (userId == 0L || sub.getUserId() == userId) && sub.getStatus().equals("active"))
                .collect(Collectors.toList());
    }

    public static List<PaddleBillingSubscription> retrieveSubscriptionsByUserId(long userId) {
        return PaddleBillingCache.getSubscriptions()
                .stream()
                .filter(sub -> userId == 0L || sub.getUserId() == userId)
                .collect(Collectors.toList());
    }

}
