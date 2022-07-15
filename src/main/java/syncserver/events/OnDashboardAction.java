package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "DASH_ACTION")
public class OnDashboardAction implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnDashboardAction.class);

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long guildId = jsonObject.getLong("guild_id");
        Cluster cluster = ClusterConnectionManager.getResponsibleCluster(guildId);
        try {
            return cluster.send("DASH_ACTION", jsonObject).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error", e);
        }
        return null;
    }

}