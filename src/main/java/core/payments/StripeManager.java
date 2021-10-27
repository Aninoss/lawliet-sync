package core.payments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;

public class StripeManager {

    public static Map<Long, ArrayList<Subscription>> retrieveActiveSubscriptions() throws StripeException {
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

    public static List<Subscription> retrieveActiveSubscriptions(long userId) throws StripeException {
        return Subscription.list(SubscriptionListParams.builder()
                        .setStatus(SubscriptionListParams.Status.ACTIVE)
                        .build()
                )
                .getData()
                .stream()
                .filter(sub -> userId == 0L ||
                        (sub.getMetadata().containsKey("discord_id") && sub.getMetadata().get("discord_id").equals(String.valueOf(userId)))
                )
                .collect(Collectors.toList());
    }

}
