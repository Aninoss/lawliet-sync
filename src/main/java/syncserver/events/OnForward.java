package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import core.ExceptionLogger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "FORWARD")
public class OnForward implements SyncServerFunction {

    private final static int TIMEOUT_SECONDS = 4;
    private final static Logger LOGGER = LoggerFactory.getLogger(OnForward.class);

    private enum ForwardType { ALL_CLUSTERS, ANY_CLUSTER, SPECIFIC_CLUSTER, SPECIFIC_GUILD }

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        ForwardType forwardType = ForwardType.valueOf(jsonObject.getString("forward_type"));
        String event = jsonObject.getString("event");
        int forwardToClusterId = jsonObject.has("cluster_id") ? jsonObject.getInt("cluster_id") : 1;
        long guildId = jsonObject.has("guild_id") ? jsonObject.getLong("guild_id") : 0L;

        try {
            return switch (forwardType) {
                case ALL_CLUSTERS -> {
                    for (Cluster cluster : ClusterConnectionManager.getFullyConnectedClusters()) {
                        cluster.send(event, jsonObject).exceptionally(ExceptionLogger.get());
                    }
                    yield null;
                }
                case ANY_CLUSTER -> ClusterConnectionManager.getFirstFullyConnectedCluster().get()
                        .send(event, jsonObject).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                case SPECIFIC_CLUSTER -> ClusterConnectionManager.getCluster(forwardToClusterId)
                        .send(event, jsonObject).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                case SPECIFIC_GUILD -> ClusterConnectionManager.getResponsibleCluster(guildId)
                        .send(event, jsonObject).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            };
        } catch (InterruptedException e) {
            LOGGER.error("Request {} interrupted", event);
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.error("Request {} error", event, e);
        }

        return null;
    }

}