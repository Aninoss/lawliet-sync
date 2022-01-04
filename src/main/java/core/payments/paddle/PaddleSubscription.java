package core.payments.paddle;

public class PaddleSubscription {

    private final int subId;
    private final long userId;
    private final boolean unlocksServer;
    private final int quantity;

    public PaddleSubscription(int subId, long userId, boolean unlocksServer, int quantity) {
        this.subId = subId;
        this.userId = userId;
        this.unlocksServer = unlocksServer;
        this.quantity = quantity;
    }

    public int getSubId() {
        return subId;
    }

    public long getUserId() {
        return userId;
    }

    public boolean unlocksServer() {
        return unlocksServer;
    }

    public int getQuantity() {
        return quantity;
    }

}
