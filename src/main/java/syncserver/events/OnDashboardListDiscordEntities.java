package syncserver.events;

import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "DASH_LIST_DISCORD_ENTITIES")
public class OnDashboardListDiscordEntities implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            long guildId = jsonObject.getLong("guild_id");
            Cluster cluster = ClusterConnectionManager.getInstance().getResponsibleCluster(guildId);
            return SendEvent.sendJSON("DASH_LIST_DISCORD_ENTITIES", cluster.getClusterId(), jsonObject).join();
        }
        return null;
    }

}