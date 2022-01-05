package core.payments.paddle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PaddleManager {

    public static Map<Long, ArrayList<PaddleSubscription>> retrieveActiveSubscriptions() {
        HashMap<Long, ArrayList<PaddleSubscription>> map = new HashMap<>();
        for (PaddleSubscription subscription : retrieveActiveSubscriptions(0L)) {
            long userId = subscription.getUserId();
            ArrayList<PaddleSubscription> subscriptions = map.computeIfAbsent(userId, k -> new ArrayList<>());
            subscriptions.add(subscription);
            map.put(userId, subscriptions);
        }
        return map;
    }

    public static List<PaddleSubscription> retrieveActiveSubscriptions(long userId) {
        return PaddleCache.getSubscriptions()
                .stream()
                .filter(sub -> (userId == 0L || sub.getUserId() == userId) && sub.getStatus().equals("active"))
                .collect(Collectors.toList());
    }

    public static List<PaddleSubscription> retrieveSubscriptions(long userId) {
        return PaddleCache.getSubscriptions()
                .stream()
                .filter(sub -> userId == 0L || sub.getUserId() == userId)
                .collect(Collectors.toList());
    }

}
