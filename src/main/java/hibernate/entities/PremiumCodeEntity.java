package hibernate.entities;

import hibernate.InstantConverter;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "PremiumCode")
public class PremiumCodeEntity {

    public enum Level { BASIC, PRO }

    @Id
    private String code;

    @Enumerated(EnumType.STRING)
    private Level level;

    private int durationDays;
    private long boughtByUserId;

    @Convert(converter = InstantConverter.class)
    private Instant createdTime = Instant.now();

    private Long redeemedByUserId = null;

    @Convert(converter = InstantConverter.class)
    private Instant expiration = null;

    public PremiumCodeEntity() {
    }

    public PremiumCodeEntity(String code, Level level, int durationDays, long boughtByUserId) {
        this.code = code;
        this.level = level;
        this.durationDays = durationDays;
        this.boughtByUserId = boughtByUserId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public long getBoughtByUserId() {
        return boughtByUserId;
    }

    public void setBoughtByUserId(long boughtUserId) {
        this.boughtByUserId = boughtUserId;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Instant createdTime) {
        this.createdTime = createdTime;
    }

    public Long getRedeemedByUserId() {
        return redeemedByUserId;
    }

    public void setRedeemedByUserId(Long redeemedUserId) {
        this.redeemedByUserId = redeemedUserId;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public boolean isRedeemed() {
        return redeemedByUserId != null;
    }

    public boolean isActive() {
        return isRedeemed() && Instant.now().isBefore(expiration);
    }

    public static List<PremiumCodeEntity> findAllBoughtByUserId(EntityManager entityManager, long userId) {
        return entityManager.createQuery("FROM PremiumCode WHERE boughtByUserId = :userId", PremiumCodeEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public static List<PremiumCodeEntity> findAllRedeemedByUserId(EntityManager entityManager, long userId) {
        return entityManager.createQuery("FROM PremiumCode WHERE redeemedByUserId = :userId", PremiumCodeEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public static List<PremiumCodeEntity> findAllActiveRedeemedByUserId(EntityManager entityManager, long userId) {
        return entityManager.createQuery("FROM PremiumCode WHERE redeemedByUserId = :userId", PremiumCodeEntity.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .filter(PremiumCodeEntity::isActive)
                .collect(Collectors.toList());
    }

    public static List<PremiumCodeEntity> findAllActive(EntityManager entityManager) {
        return entityManager.createQuery("FROM PremiumCode", PremiumCodeEntity.class)
                .getResultList()
                .stream()
                .filter(PremiumCodeEntity::isActive)
                .collect(Collectors.toList());
    }

}
