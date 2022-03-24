package syncserver.events;

import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SERVER_NAME")
public class OnRequestServerName implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long serverId = jsonObject.getLong("server_id");
        JSONObject responseJson = new JSONObject();
        ClusterConnectionManager.getResponsibleCluster(serverId).fetchServerNameById(serverId)
                .ifPresent(name -> responseJson.put("name", name));
        return responseJson;
    }

}