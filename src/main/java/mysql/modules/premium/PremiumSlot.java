package mysql.modules.premium;

public class PremiumSlot {

    private final long userId;
    private final int slot;
    private final long guildId;

    public PremiumSlot(long userId, int slot, long guildId) {
        this.userId = userId;
        this.slot = slot;
        this.guildId = guildId;
    }

    public long getUserId() {
        return userId;
    }

    public int getSlot() {
        return slot;
    }

    public long getGuildId() {
        return guildId;
    }

}
