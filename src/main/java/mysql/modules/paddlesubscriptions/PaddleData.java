package mysql.modules.paddlesubscriptions;

public class PaddleData {

    private final long subId;
    private final long userId;
    private final boolean unlocksServer;

    public PaddleData(long subId, long userId, boolean unlocksServer) {
        this.subId = subId;
        this.userId = userId;
        this.unlocksServer = unlocksServer;
    }

    public long getSubId() {
        return subId;
    }

    public long getUserId() {
        return userId;
    }

    public boolean unlocksServer() {
        return unlocksServer;
    }

}
