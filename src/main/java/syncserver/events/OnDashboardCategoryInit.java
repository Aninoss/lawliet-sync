package syncserver.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "DASH_CAT_INIT")
public class OnDashboardCategoryInit implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnDashboardCategoryInit.class);

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            long guildId = jsonObject.getLong("guild_id");
            Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(guildId);
            try {
                return SendEvent.sendJSON("DASH_CAT_INIT", cluster.getClusterId(), jsonObject).get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.error("Error", e);
            }
        }
        return null;
    }

}