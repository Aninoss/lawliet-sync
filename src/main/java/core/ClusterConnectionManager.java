package core;

import core.cache.DiscordRecommendedTotalShardsCache;
import core.schedule.MainScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClusterConnectionManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterConnectionManager.class);

    private final HashMap<Integer, Cluster> clusterMap = new HashMap<>();
    private final ConcurrentLinkedQueue<Cluster> clusterConnectQueue = new ConcurrentLinkedQueue<>();
    private CustomThread clusterConnectQueueTaskThread = null;
    private boolean started = false;

    public void register(int clusterId, int size, boolean alreadyConnected) {
        Cluster cluster = clusterMap.get(clusterId);
        if (cluster == null) {
            LOGGER.info("Adding new cluster with id {} and size {}", clusterId, size);
            cluster = new Cluster(clusterId, size);
            clusterMap.put(clusterId, cluster);
        } else {
            LOGGER.info("Connection with cluster {} has been reestablished", clusterId);
        }
        cluster.connectionStatus = alreadyConnected ? Cluster.ConnectionStatus.FULLY_CONNECTED : Cluster.ConnectionStatus.BOOTING_UP;
        if (!alreadyConnected)
            enqueueClusterConnection(cluster);
    }

    public void unregister(int clusterId) {
        clusterMap.computeIfPresent(clusterId, (cId, cluster) -> {
            LOGGER.info("Cluster {} has been unregistered", cId);
            cluster.connectionStatus = Cluster.ConnectionStatus.OFFLINE;
            return cluster;
        });
    }

    public void connected(int clusterId) {
        clusterMap.computeIfPresent(clusterId, (cId, cluster) -> {
            LOGGER.info("Cluster {} is connected", cId);
            cluster.connectionStatus = Cluster.ConnectionStatus.FULLY_CONNECTED;
            return cluster;
        });
    }

    public void start() {
        int totalShards = DiscordRecommendedTotalShardsCache.getInstance().getAsync();
        LOGGER.info("Starting clusters with total shard size of {}", totalShards);
        List<Cluster> clusters = new ArrayList<>(clusterMap.values());
        clusters.sort(Comparator.comparingInt(c -> c.clusterId));
        started = true;

        int shift = 0;
        int totalSize = getTotalSize();
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);

            int area;
            if (i < clusters.size() - 1)
                area = (int) Math.round(((double)cluster.size / totalSize) * totalShards);
            else
                area = totalShards - shift;

            cluster.shardInterval = new int[]{ shift, shift + area - 1 };
            LOGGER.info("Cluster {} uses shards {} to {}", cluster.clusterId, cluster.shardInterval[0], cluster.shardInterval[1]);
            enqueueClusterConnection(cluster);
            shift += area;
        }
    }

    public synchronized void enqueueClusterConnection(Cluster cluster) {
        if (started && !clusterConnectQueue.contains(cluster) && cluster.shardInterval != null) {
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
                .mapToInt(c -> c.size)
                .sum();
    }

    private void startClusterConnectTask() {
        Cluster cluster;
        while((cluster = clusterConnectQueue.poll()) != null) {
            switch (cluster.connectionStatus) {
                case FULLY_CONNECTED:
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                    LOGGER.info("Disconnecting cluster {}", cluster.clusterId);
                    cluster.connectionStatus = Cluster.ConnectionStatus.OFFLINE;
                    //TODO disconnect cluster
                    break;

                case BOOTING_UP:
                    LOGGER.info("Connecting cluster {}", cluster.clusterId);
                    //TODO connect cluster
                    while (cluster.connectionStatus == Cluster.ConnectionStatus.BOOTING_UP) {
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
            }
        }
    }


    private static class Cluster {

        private enum ConnectionStatus { OFFLINE, BOOTING_UP, FULLY_CONNECTED}

        private final int clusterId;
        private final int size;
        private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
        private int[] shardInterval = null;

        public Cluster(int clusterId, int size) {
            this.clusterId = clusterId;
            this.size = size;
        }

    }

}
