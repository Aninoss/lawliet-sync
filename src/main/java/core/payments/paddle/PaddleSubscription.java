package core.payments.paddle;

import java.time.LocalDate;

public class PaddleSubscription {

    private final long subId;
    private final long planId;
    private final long userId;
    private final boolean unlocksServer;
    private final int quantity;
    private final String status;
    private final String totalPrice;
    private final LocalDate nextPayment;
    private final String updateUrl;

    public PaddleSubscription(long subId, long planId, long userId, boolean unlocksServer, int quantity, String status, String totalPrice, LocalDate nextPayment, String updateUrl) {
        this.subId = subId;
        this.planId = planId;
        this.userId = userId;
        this.unlocksServer = unlocksServer;
        this.quantity = quantity;
        this.status = status;
        this.totalPrice = totalPrice;
        this.nextPayment = nextPayment;
        this.updateUrl = updateUrl;
    }

    public long getSubId() {
        return subId;
    }

    public long getPlanId() {
        return planId;
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

    public String getStatus() {
        return status;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public LocalDate getNextPayment() {
        return nextPayment;
    }

    public String getUpdateUrl() {
        return updateUrl;
    }

}
