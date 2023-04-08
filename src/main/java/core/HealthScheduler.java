package core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import club.minnced.discord.webhook.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;

public class HealthScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(HealthScheduler.class);

    public static void run() {
        if (!Program.isProductionMode()) {
            return;
        }

        WebhookClient client = WebhookClient.withUrl(System.getenv("HEALTH_WEBHOOK_URL"));
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            StringBuilder sb = new StringBuilder("<t:")
                    .append(System.currentTimeMillis() / 1000)
                    .append(":T>\n```diff\n | CL.  | STATUS\n----------------------------\n");

            boolean containsCustomInstances = false;
            for (Cluster cluster : ClusterConnectionManager.getClusters()) {
                if (!cluster.isPublicCluster()) {
                    containsCustomInstances = true;
                } else if (containsCustomInstances) {
                    containsCustomInstances = false;
                    sb.append("----------------------------\n");
                }

                boolean green = cluster.getConnectionStatus() == Cluster.ConnectionStatus.FULLY_CONNECTED &&
                        (cluster.getConnectedShards() < 0 || cluster.getConnectedShards() == 16);

                StringBuilder line = new StringBuilder(green ? "+" : "-")
                        .append("| ")
                        .append(cluster.isPublicCluster() ? "" : "C-")
                        .append(cluster.isPublicCluster() ? cluster.getClusterId() : (Math.abs(cluster.getClusterId()) - 1));

                line.append(" ".repeat(7 - line.length()))
                        .append(" | ");

                switch (cluster.getConnectionStatus()) {
                    case OFFLINE -> line.append("Offline");
                    case BOOTING_UP -> line.append("Booting Up");
                    case FULLY_CONNECTED -> line.append("Running");
                }

                if (cluster.getConnectedShards() >= 0) {
                    line.append(" (")
                            .append(cluster.getConnectedShards())
                            .append("/16)");
                }

                line.append(" ".repeat(28 - line.length()));
                sb.append(line)
                        .append('\n');
            }

            sb.append("```");
            try {
                client.edit(System.getenv("HEALTH_MESSAGE_ID"), sb.toString()).get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Message edit error", e);
            }
        }, 30, Integer.parseInt(System.getenv("HEALTH_PERIOD")), TimeUnit.SECONDS);
    }

}
