package syncserver.events;

import mysql.modules.botstats.DBBotStats;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SERVER_STATS")
public class OnServerStats implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();

        DBBotStats.getMonthlyServerStats().forEach(slot -> {
            JSONObject slotJson = new JSONObject();
            slotJson.put("month", slot.getMonth());
            slotJson.put("year", slot.getYear());
            slotJson.put("value", slot.getServerCount());
            arrayJSON.put(slotJson);
        });

        mainJSON.put("data", arrayJSON);
        mainJSON.put("servers", ClusterConnectionManager.getGlobalServerSize().orElse(null));
        return mainJSON;
    }

}