package syncserver;

public class Cluster {

    public enum ConnectionStatus { OFFLINE, BOOTING_UP, FULLY_CONNECTED }

    private final int clusterId;
    private final int size;
    private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
    private int[] shardInterval = null;

    public Cluster(int clusterId, int size) {
        this.clusterId = clusterId;
        this.size = size;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setShardInterval(int[] shardInterval) {
        this.shardInterval = shardInterval;
    }

    public int getClusterId() {
        return clusterId;
    }

    public int getSize() {
        return size;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public int[] getShardInterval() {
        return shardInterval;
    }

}
