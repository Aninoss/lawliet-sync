package syncserver;

import core.ExceptionLogger;
import core.InRelationSplitter;
import core.cache.DiscordRecommendedTotalShardsCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService connectorService = Executors.newSingleThreadExecutor();
    private Integer totalShards = null;

    public synchronized void register(int clusterId, int size) {
        LOGGER.info("Adding unconnected cluster with id: {}, size: {}, alreadyPresent: {}", clusterId, size, clusterMap.containsKey(clusterId));
        Cluster cluster = clusterMap.computeIfAbsent(clusterId, cId -> new Cluster(clusterId, size));
        cluster.setConnectionStatus(Cluster.ConnectionStatus.BOOTING_UP);
        submitConnectCluster(cluster, false, false);
    }

    public synchronized void registerAlreadyConnected(int clusterId, int size, int shardMin, int shardMax, int totalShards) {
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
        int totalShards = DiscordRecommendedTotalShardsCache.getInstance().getAsync();
        if (this.totalShards != null && this.totalShards != totalShards) {
            LOGGER.info("Shard size has changed");
        }
        this.totalShards = totalShards;
        LOGGER.info("Starting clusters with total shard size of {}", totalShards);
        List<Cluster> clusters = getClusters();

        int shift = 0;

        InRelationSplitter irs = new InRelationSplitter(totalShards);
        clusterMap.values()
                .forEach(c -> irs.insert(c.getSize()));
        int[] shards = irs.process();

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);

            int area = shards[i];
            int shardMin = shift;
            int shardMax = shift + area - 1;
            cluster.setShardInterval(new int[]{ shardMin, shardMax });
            LOGGER.info("Cluster {} uses shards {} to {}", cluster.getClusterId(), cluster.getShardInterval()[0], cluster.getShardInterval()[1]);
            submitConnectCluster(cluster, true, true);
            shift += area;
        }
    }

    public void restart() {
        if (totalShards == null) {
            start();
        } else {
            LOGGER.info("Restarting clusters with total shard size of {}", totalShards);
            getActiveClusters().forEach(cluster -> submitConnectCluster(cluster, true, false));
        }
    }

    public void submitConnectCluster(Cluster cluster, boolean allowReconnect, boolean sendBlockShards) {
        if (cluster.isActive() && totalShards != null) {
            connectorService.submit(() -> {
                try {
                    connectCluster(cluster, allowReconnect, sendBlockShards);
                } catch (InterruptedException e) {
                    //Ignore
                }
            });
        }
    }

    private void connectCluster(Cluster cluster, boolean allowReconnect, boolean sendBlockShards) throws InterruptedException {
        if (allowReconnect && cluster.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED) {
            LOGGER.info("Disconnecting cluster {}", cluster.getClusterId());
            cluster.setConnectionStatus(Cluster.ConnectionStatus.OFFLINE);
            SendEvent.sendExit(cluster.getClusterId());
        }

        while(cluster.getConnectionStatus() != Cluster.ConnectionStatus.FULLY_CONNECTED) {
            Thread.sleep(100);

            while (cluster.getConnectionStatus() == Cluster.ConnectionStatus.OFFLINE) {
                Thread.sleep(100);
            }

            LOGGER.info("Connecting cluster {}", cluster.getClusterId());

            if (sendBlockShards) {
                ClusterConnectionManager.getInstance().getActiveClusters().stream()
                        .filter(c -> c.getClusterId() > cluster.getClusterId())
                        .forEach(c -> SendEvent.sendBlockShards(c.getClusterId(), totalShards, cluster.getShardInterval()[0], cluster.getShardInterval()[1]));
            }

            cluster.setConnectionStatus(Cluster.ConnectionStatus.BOOTING_UP);
            SendEvent.sendStartConnection(cluster.getClusterId(), cluster.getShardInterval()[0], cluster.getShardInterval()[1], totalShards)
                    .exceptionally(ExceptionLogger.get());

            while (cluster.getConnectionStatus() == Cluster.ConnectionStatus.BOOTING_UP) {
                Thread.sleep(100);
            }
        }
    }

    public Optional<Integer> getTotalShards() {
        return Optional.ofNullable(totalShards);
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

    public int getResponsibleShard(long serverId) {
        return Math.abs((int) ((serverId >> 22) % totalShards));
    }

}
