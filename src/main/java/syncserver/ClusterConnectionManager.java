package syncserver;

import core.CustomThread;
import core.cache.DiscordRecommendedTotalShardsCache;
import core.util.StringUtil;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class ClusterConnectionManager {

    public final static long OWNER_ID = 272037078919938058L;
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
        if (!clusterConnectQueue.contains(cluster) && cluster.isActive() && totalShards != null) {
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

    public Optional<Long> getGlobalServerSize() {
        long globalServerSize = 0;
        for(Cluster cluster : ClusterConnectionManager.getInstance().getClusters()) {
            if (cluster.isActive() || cluster.getLocalServerSize().isPresent()) {
                if (cluster.getLocalServerSize().isEmpty()) {
                    globalServerSize = 0;
                    break;
                }
                globalServerSize += cluster.getLocalServerSize().get();
            }
        }
        return globalServerSize > 0 ? Optional.of(globalServerSize) : Optional.empty();
    }

    public Cluster getCluster(int clusterId) {
        return clusterMap.get(clusterId);
    }

    public List<Cluster> getClusters() {
        ArrayList<Cluster> clusters = new ArrayList<>(clusterMap.values());
        clusters.sort(Comparator.comparingInt(Cluster::getClusterId));
        return clusters;
    }

    public List<Cluster> getActiveClusters() {
        return clusterMap.values().stream()
                .filter(Cluster::isActive)
                .sorted(Comparator.comparingInt(Cluster::getClusterId))
                .collect(Collectors.toList());
    }

    public Optional<Cluster> getFirstFullyConnectedCluster() {
        return clusterMap.values().stream()
                .filter(c -> c.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED)
                .findFirst();
    }

    public Cluster getResponsibleCluster(long serverId) {
        int shard = getResponsibleShard(serverId);
        for (Cluster cluster : getActiveClusters()) {
            int[] shardInterval = cluster.getShardInterval();
            if (shard >= shardInterval[0] && shard <= shardInterval[1])
                return cluster;
        }

        throw new RuntimeException("Unknown error");
    }

    private int getResponsibleShard(long serverId) {
        return Math.abs((int) ((serverId >> 22) % totalShards));
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
                    SendEvent.sendStartConnection(cluster.getClusterId(), cluster.getShardInterval()[0], cluster.getShardInterval()[1], totalShards)
                            .exceptionally(ExceptionLogger.get());
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
