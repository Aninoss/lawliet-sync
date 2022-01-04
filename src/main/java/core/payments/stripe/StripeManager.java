package core.payments.stripe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.stripe.model.Subscription;

public class StripeManager {

    public static Map<Long, ArrayList<Subscription>> retrieveActiveSubscriptions() {
        HashMap<Long, ArrayList<Subscription>> map = new HashMap<>();
        for (Subscription subscription : retrieveActiveSubscriptions(0L)) {
            if (subscription.getMetadata().containsKey("discord_id")) {
                long userId = Long.parseLong(subscription.getMetadata().get("discord_id"));
                ArrayList<Subscription> subscriptions = map.computeIfAbsent(userId, k -> new ArrayList<>());
                subscriptions.add(subscription);
                map.put(userId, subscriptions);
            }
        }
        return map;
    }

    public static List<Subscription> retrieveActiveSubscriptions(long userId) {
        return StripeCache.getSubscriptions()
                .stream()
                .filter(sub -> userId == 0L ||
                        (sub.getMetadata().containsKey("discord_id") && sub.getMetadata().get("discord_id").equals(String.valueOf(userId)))
                )
                .collect(Collectors.toList());
    }

}
