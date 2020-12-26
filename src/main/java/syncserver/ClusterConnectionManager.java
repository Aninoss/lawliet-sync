package syncserver;

import core.CustomThread;
import core.cache.DiscordRecommendedTotalShardsCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClusterConnectionManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterConnectionManager.class);

    private static final ClusterConnectionManager ourInstance = new ClusterConnectionManager();

    public static ClusterConnectionManager getInstance() {
        return ourInstance;
    }

    private ClusterConnectionManager() {
    }

    private final HashMap<Integer, Cluster> clusterMap = new HashMap<>();
    private final ConcurrentLinkedQueue<Cluster> clusterConnectQueue = new ConcurrentLinkedQueue<>();
    private CustomThread clusterConnectQueueTaskThread = null;
    private Integer totalShards = null;

    public void register(int clusterId, int size) {
        LOGGER.info("Adding unconnected cluster with id: {}, size: {}, alreadyPresent: {}", clusterId, size, clusterMap.containsKey(clusterId));
        Cluster cluster = clusterMap.computeIfAbsent(clusterId, cId -> new Cluster(clusterId, size));
        cluster.setConnectionStatus(Cluster.ConnectionStatus.BOOTING_UP);
        enqueueClusterConnection(cluster);
    }

    public void registerAlreadyConnected(int clusterId, int size, int shardMin, int shardMax, int totalShards) {
        LOGGER.info("Adding connected cluster with id: {}, size: {}, alreadyPresent: {}", clusterId, size, clusterMap.containsKey(clusterId));
        Cluster cluster = clusterMap.computeIfAbsent(clusterId, cId -> new Cluster(clusterId, size));
        cluster.setShardInterval(new int[] { shardMin, shardMax });
        cluster.setConnectionStatus(Cluster.ConnectionStatus.FULLY_CONNECTED);
        if (this.totalShards == null)
            this.totalShards = totalShards;
    }

    public void unregister(int clusterId) {
        clusterMap.computeIfPresent(clusterId, (cId, cluster) -> {
            LOGGER.info("Cluster {} has been unregistered", cId);
            cluster.setConnectionStatus(Cluster.ConnectionStatus.OFFLINE);
            return cluster;
        });
    }

    public void connected(int clusterId) {
        clusterMap.computeIfPresent(clusterId, (cId, cluster) -> {
            LOGGER.info("Cluster {} is fully connected", cId);
            cluster.setConnectionStatus(Cluster.ConnectionStatus.FULLY_CONNECTED);
            return cluster;
        });
    }

    public void start() {
        totalShards = DiscordRecommendedTotalShardsCache.getInstance().getAsync();
        LOGGER.info("Starting clusters with total shard size of {}", totalShards);
        List<Cluster> clusters = getClusters();

        int shift = 0;
        int totalSize = getTotalSize();
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);

            int area;
            if (i < clusters.size() - 1)
                area = (int) Math.round(((double) cluster.getSize() / totalSize) * totalShards);
            else
                area = totalShards - shift;

            int shardMin = shift;
            int shardMax = shift + area - 1;
            cluster.setShardInterval(new int[]{ shardMin, shardMax });
            LOGGER.info("Cluster {} uses shards {} to {}", cluster.getClusterId(), cluster.getShardInterval()[0], cluster.getShardInterval()[1]);
            enqueueClusterConnection(cluster);
            shift += area;
        }
    }

    public synchronized void enqueueClusterConnection(Cluster cluster) {
        if (!clusterConnectQueue.contains(cluster) && cluster.getShardInterval() != null && totalShards != null) {
            clusterConnectQueue.add(cluster);
            if (clusterConnectQueueTaskThread == null || !clusterConnectQueueTaskThread.isAlive()) {
                clusterConnectQueueTaskThread = new CustomThread(this::startClusterConnectTask, "cluster_connect", 1);
                clusterConnectQueueTaskThread.start();
            }
        }
    }

    public int getTotalSize() {
        return clusterMap
                .values()
                .stream()
                .mapToInt(Cluster::getSize)
                .sum();
    }

    public List<Cluster> getClusters() {
        ArrayList<Cluster> clusters = new ArrayList<>(clusterMap.values());
        clusters.sort(Comparator.comparingInt(Cluster::getClusterId));
        return clusters;
    }

    private void startClusterConnectTask() {
        Cluster cluster;
        while ((cluster = clusterConnectQueue.poll()) != null) {
            switch (cluster.getConnectionStatus()) {
                case FULLY_CONNECTED:
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                    LOGGER.info("Disconnecting cluster {}", cluster.getClusterId());
                    cluster.setConnectionStatus(Cluster.ConnectionStatus.OFFLINE);
                    SendEvent.sendExit(cluster.getClusterId());
                    break;

                case BOOTING_UP:
                    LOGGER.info("Connecting cluster {}", cluster.getClusterId());
                    SendEvent.sendStartConnection(cluster.getClusterId(), cluster.getShardInterval()[0], cluster.getShardInterval()[1], totalShards);
                    while (cluster.getConnectionStatus() == Cluster.ConnectionStatus.BOOTING_UP) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            LOGGER.error("Interrupted", e);
                        }
                    }
                    break;

                case OFFLINE:
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                    break;

                default:
                    throw new NoSuchElementException("Invalid connection status");
            }
        }
    }

}
