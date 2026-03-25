package core.payments.paddlebilling;

import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaddleBillingSubscription {

    private final String subscriptionId;
    private final String customerId;
    private final int quantity;
    private final String status;
    private final long userId;
    private final boolean unlocksGuilds;
    private final List<Long> presetGuilds;

    public PaddleBillingSubscription(String subscriptionId, String customerId, int quantity, String status, long userId, boolean unlocksGuilds, List<Long> presetGuilds) {
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
        this.userId = userId;
        this.unlocksGuilds = unlocksGuilds;
        this.presetGuilds = presetGuilds;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    public long getUserId() {
        return userId;
    }

    public boolean getUnlocksGuilds() {
        return unlocksGuilds;
    }

    public List<Long> getPresetGuilds() {
        return presetGuilds;
    }

    public static PaddleBillingSubscription fromJson(JSONObject data) {
        JSONObject customData = data.getJSONObject("custom_data");
        JSONObject priceCustomData = data.getJSONArray("items").getJSONObject(0).getJSONObject("price").getJSONObject("custom_data");
        return new PaddleBillingSubscription(
                data.getString("id"),
                data.getString("customer_id"),
                data.getJSONArray("items").getJSONObject(0).getInt("quantity"),
                data.getString("status"),
                customData.getLong("discord_id"),
                priceCustomData.getBoolean("unlocks_guilds"),
                customData.isNull("preset_guilds") || customData.getString("preset_guilds").isEmpty() ? null : Stream.of(customData.getString("preset_guilds").split(",")).map(Long::valueOf).collect(Collectors.toList())
        );
    }

}
