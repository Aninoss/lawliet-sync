package hibernate.entities;

import hibernate.InstantConverter;

import javax.persistence.Convert;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordSubscriptionEntity {

    public enum SKU { BASIC }

    @Id
    private final String id;

    private long userId = 0;

    @Convert(converter = InstantConverter.class)
    private Instant timeEnding = null;

    private SKU sku = SKU.BASIC;


    public DiscordSubscriptionEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Instant getTimeEnding() {
        return timeEnding;
    }

    public void setTimeEnding(Instant timeEnding) {
        this.timeEnding = timeEnding;
    }

    public SKU getSku() {
        return sku;
    }

    public void setSku(SKU sku) {
        this.sku = sku;
    }

    public static List<DiscordSubscriptionEntity> findValidDiscordSubscriptionEntitiesByUserId(EntityManager entityManager, long userId) {
        return ((List<DiscordSubscriptionEntity>) entityManager.createNativeQuery("{'userId': NumberLong('" + userId + "')}", DiscordSubscriptionEntity.class).getResultList()).stream()
                .filter(entity -> entity.getTimeEnding() == null || entity.getTimeEnding().isAfter(Instant.now()))
                .collect(Collectors.toList());
    }

}
