package mysql.modules.paddlesubscriptions;

public class PaddleData {

    private final int subId;
    private final long userId;
    private final boolean unlocksServer;

    public PaddleData(int subId, long userId, boolean unlocksServer) {
        this.subId = subId;
        this.userId = userId;
        this.unlocksServer = unlocksServer;
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

}
