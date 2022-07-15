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

    private final static Logger LOGGER = LoggerFactory.getLogger(OnForward.class);

    private enum ForwardType { ALL_CLUSTERS, ANY_CLUSTER, FIRST_CLUSTER, SERVER_CLUSTER }

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        ForwardType forwardType = ForwardType.valueOf(jsonObject.getString("forward_type"));
        String event = jsonObject.getString("event");

        try {
            return switch (forwardType) {
                case ALL_CLUSTERS -> {
                    for (Cluster cluster : ClusterConnectionManager.getFullyConnectedClusters()) {
                        cluster.send(event, jsonObject).exceptionally(ExceptionLogger.get());
                    }
                    yield null;
                }
                case ANY_CLUSTER -> ClusterConnectionManager.getFirstFullyConnectedCluster().get()
                        .send(event, jsonObject).get(3, TimeUnit.SECONDS);
                case FIRST_CLUSTER -> ClusterConnectionManager.getCluster(1)
                        .send(event, jsonObject).get(3, TimeUnit.SECONDS);
                case SERVER_CLUSTER -> ClusterConnectionManager.getResponsibleCluster(jsonObject.getLong("guild_id"))
                        .send(event, jsonObject).get(3, TimeUnit.SECONDS);
            };
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error", e);
        }

        return null;
    }

}