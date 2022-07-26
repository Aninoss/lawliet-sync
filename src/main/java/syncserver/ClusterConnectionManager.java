package syncserver;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import core.ExceptionLogger;
import core.Program;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterConnectionManager {

    public final static long OWNER_ID = 272037078919938058L;
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterConnectionManager.class);

    private static final HashMap<Integer, Cluster> clusterMap = new HashMap<>();
    private static final ExecutorService connectorService = Executors.newSingleThreadExecutor();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private static Integer totalShards = null;
    private static final HashMap<Integer, Instant> clusterCache = new HashMap<>();

    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (int clusterId : new ArrayList<>(clusterCache.keySet())) {
                if (clusterCache.get(clusterId).isBefore(Instant.now().minusSeconds(5))) {
                    clusterCache.remove(clusterId);
                    unregister(clusterId);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static boolean heartbeat(int clusterId) {
        return clusterCache.put(clusterId, Instant.now()) == null;
    }

    public static synchronized void clusterSetFullyConnected(int clusterId, int totalShards, String ip) {
        Cluster cluster = clusterMap.computeIfAbsent(clusterId, cId -> new Cluster(clusterId, ip));
        if (cluster.getConnectionStatus() != Cluster.ConnectionStatus.FULLY_CONNECTED) {
            LOGGER.info("Cluster {} is fully connected", clusterId);
            cluster.setConnectionStatus(Cluster.ConnectionStatus.FULLY_CONNECTED);
            if (ClusterConnectionManager.totalShards == null) {
                ClusterConnectionManager.totalShards = totalShards;
            }
        }
    }

    public static synchronized void registerUnconnectedCluster(int clusterId, String ip) {
        LOGGER.info("Adding unconnected cluster with id: {}, alreadyPresent: {}", clusterId, clusterMap.containsKey(clusterId));
        Cluster cluster = clusterMap.computeIfAbsent(clusterId, cId -> new Cluster(clusterId, ip));
        cluster.setConnectionStatus(Cluster.ConnectionStatus.BOOTING_UP);
        submitConnectCluster(cluster, false, false);
    }

    public static synchronized void registerConnectedCluster(int clusterId, int totalShards, String ip) {
        LOGGER.info("Adding connected cluster with id: {}, alreadyPresent: {}", clusterId, clusterMap.containsKey(clusterId));
        clusterSetFullyConnected(clusterId, totalShards, ip);
    }

    public static void unregister(int clusterId) {
        clusterMap.computeIfPresent(clusterId, (cId, cluster) -> {
            LOGGER.info("Cluster {} has been unregistered", cId);
            cluster.setConnectionStatus(Cluster.ConnectionStatus.OFFLINE);
            return cluster;
        });
    }

    public static void start() {
        List<Cluster> clusters = getClusters();
        int totalShards = Program.isProductionMode() ? clusters.size() * 16 : 1;
        if (ClusterConnectionManager.totalShards != null && ClusterConnectionManager.totalShards != totalShards) {
            LOGGER.info("Shard size has changed");
        }
        ClusterConnectionManager.totalShards = totalShards;
        LOGGER.info("Starting clusters with total shard size of {}", totalShards);

        for (Cluster cluster : clusters) {
            LOGGER.info("Cluster {} uses shards {} to {}", cluster.getClusterId(), cluster.getShardIntervalMin(), cluster.getShardIntervalMax());
            submitConnectCluster(cluster, true, true);
        }
    }

    public static void submitConnectCluster(Cluster cluster, boolean allowReconnect, boolean sendBlockShards) {
        if (totalShards != null) {
            connectorService.submit(() -> {
                try {
                    connectCluster(cluster, allowReconnect, sendBlockShards);
                } catch (InterruptedException e) {
                    //ignore
                }
            });
        } else {
            scheduledExecutor.schedule(() -> submitConnectCluster(cluster, allowReconnect, sendBlockShards), 1, TimeUnit.SECONDS);
        }
    }

    private static void connectCluster(Cluster cluster, boolean allowReconnect, boolean sendBlockShards) throws InterruptedException {
        if (allowReconnect && cluster.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED) {
            LOGGER.info("Disconnecting cluster {}", cluster.getClusterId());
            cluster.setConnectionStatus(Cluster.ConnectionStatus.OFFLINE);
            cluster.send(EventOut.EXIT).exceptionally(ExceptionLogger.get());
        }

        while (cluster.getConnectionStatus() != Cluster.ConnectionStatus.FULLY_CONNECTED) {
            Thread.sleep(100);
            while (cluster.getConnectionStatus() == Cluster.ConnectionStatus.OFFLINE) {
                Thread.sleep(100);
            }

            LOGGER.info("Connecting cluster {}", cluster.getClusterId());

            if (sendBlockShards) {
                ClusterConnectionManager.getFullyConnectedClusters().stream()
                        .filter(c -> c.getClusterId() > cluster.getClusterId())
                        .forEach(c -> {
                            JSONObject dataJson = new JSONObject();
                            dataJson.put("total_shards", totalShards);
                            dataJson.put("shards_min", cluster.getShardIntervalMin());
                            dataJson.put("shards_max", cluster.getShardIntervalMax());
                            c.send(EventOut.BLOCK_SHARDS, dataJson).exceptionally(ExceptionLogger.get());
                        });
            }

            cluster.setConnectionStatus(Cluster.ConnectionStatus.BOOTING_UP);
            while(true) {
                try {
                    JSONObject dataJson = new JSONObject();
                    dataJson.put("shard_min", cluster.getShardIntervalMin());
                    dataJson.put("shard_max", cluster.getShardIntervalMax());
                    dataJson.put("total_shards", totalShards);
                    cluster.send(EventOut.START_CONNECTION, dataJson).get(1, TimeUnit.SECONDS);
                    break;
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.error("Cluster connection signal error, trying again...");
                    Thread.sleep(100);
                }
            }

            while (cluster.getConnectionStatus() == Cluster.ConnectionStatus.BOOTING_UP) {
                Thread.sleep(100);
            }
        }
    }

    public static Optional<Integer> getTotalShards() {
        return Optional.ofNullable(totalShards);
    }

    public static Optional<Long> getGlobalServerSize() {
        long globalServerSize = 0;
        for (Cluster cluster : ClusterConnectionManager.getClusters()) {
            if (cluster.getLocalServerSize().isPresent()) {
                if (cluster.getLocalServerSize().isEmpty()) {
                    globalServerSize = 0;
                    break;
                }
                globalServerSize += cluster.getLocalServerSize().get();
            }
        }
        return globalServerSize > 0 ? Optional.of(globalServerSize) : Optional.empty();
    }

    public static Cluster getCluster(int clusterId) {
        return clusterMap.get(clusterId);
    }

    public static List<Cluster> getClusters() {
        return clusterMap.values().stream()
                .sorted(Comparator.comparingInt(Cluster::getClusterId))
                .collect(Collectors.toList());
    }

    public static List<Cluster> getFullyConnectedClusters() {
        return clusterMap.values().stream()
                .filter(cluster -> cluster.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED)
                .sorted(Comparator.comparingInt(Cluster::getClusterId))
                .collect(Collectors.toList());
    }

    public static Optional<Cluster> getFirstFullyConnectedCluster() {
        return clusterMap.values().stream()
                .filter(c -> c.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED)
                .findFirst();
    }

    public static Cluster getResponsibleCluster(long serverId) {
        int shard = getResponsibleShard(serverId);
        for (Cluster cluster : getClusters()) {
            if (shard >= cluster.getShardIntervalMin() && shard <= cluster.getShardIntervalMax()) {
                return cluster;
            }
        }

        throw new RuntimeException("Unknown error");
    }

    public static int getResponsibleShard(long serverId) {
        return Math.abs((int) ((serverId >> 22) % totalShards));
    }

}
