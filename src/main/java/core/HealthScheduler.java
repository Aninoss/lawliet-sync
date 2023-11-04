package core;

import club.minnced.discord.webhook.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(HealthScheduler.class);

    public static void run() {
        if (!Program.isProductionMode()) {
            return;
        }

        WebhookClient client = WebhookClient.withUrl(System.getenv("HEALTH_WEBHOOK_URL"));
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                client.edit(System.getenv("HEALTH_MESSAGE_ID"), generateClusterContent(Integer.MIN_VALUE, -2)).exceptionally(ExceptionLogger.get());
                client.edit(System.getenv("HEALTH_MESSAGE_ID_2"), generateClusterContent(1, Integer.MAX_VALUE)).exceptionally(ExceptionLogger.get());
            } catch (Throwable e) {
                LOGGER.error("Health report error", e);
            }
        }, 30, Integer.parseInt(System.getenv("HEALTH_PERIOD")), TimeUnit.SECONDS);
    }

    private static String generateClusterContent(int clusterIdMin, int clusterIdMax) {
        StringBuilder sb = new StringBuilder("<t:")
                .append(System.currentTimeMillis() / 1000)
                .append(":T>\n```diff\n | CL.  | STATUS (SHARDS)\n----------------------------\n");

        if (clusterIdMin < 1 && clusterIdMax < 1) {
            List<Cluster> customClusters = ClusterConnectionManager.getCustomClusters();
            for (Cluster cluster : customClusters) {
                if (cluster.getClusterId() >= clusterIdMin && cluster.getClusterId() <= clusterIdMax) {
                    String line = generateClusterLine(cluster);
                    sb.append(line)
                            .append('\n');
                }
            }
        } else {
            List<Cluster> publicClusters = ClusterConnectionManager.getPublicClusters();
            for (Cluster cluster : publicClusters) {
                if (cluster.getClusterId() >= clusterIdMin && cluster.getClusterId() <= clusterIdMax) {
                    String line = generateClusterLine(cluster);
                    sb.append(line)
                            .append('\n');
                }
            }
        }

        sb.append("```");
        return sb.toString();
    }

    private static String generateClusterLine(Cluster cluster) {
        boolean green = cluster.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED &&
                (cluster.getConnectedShards() == -1 || cluster.getAllShardsConnected() || !cluster.isPublicCluster());

        String clusterIdString = cluster.isPublicCluster()
                ? String.valueOf(cluster.getClusterId())
                : "C-" + (Math.abs(cluster.getClusterId()) - 1);

        StringBuilder line = new StringBuilder(green ? "+" : "-")
                .append("| ")
                .append(" ".repeat(4 - clusterIdString.length()))
                .append(clusterIdString)
                .append(" | ");

        switch (cluster.getConnectionStatus()) {
            case OFFLINE -> line.append("Offline");
            case BOOTING_UP -> line.append("Booting Up");
            case FULLY_CONNECTED -> line.append("Running");
        }

        if (cluster.getConnectedShards() != -1 &&
                cluster.getConnectionStatus() != Cluster.ConnectionStatus.OFFLINE &&
                cluster.isPublicCluster()
        ) {
            line.append(" (")
                    .append(cluster.getConnectedShards())
                    .append("/")
                    .append(ClusterConnectionManager.getShardsPerCluster())
                    .append(")");
        }

        line.append(" ".repeat(28 - line.length()));
        return line.toString();
    }

}
