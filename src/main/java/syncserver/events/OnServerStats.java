package syncserver.events;

import mysql.modules.botstats.DBBotStats;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "SERVER_STATS")
public class OnServerStats implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.equals(ClientTypes.WEB)) {
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
            mainJSON.put("servers", ClusterConnectionManager.getInstance().getGlobalServerSize().orElse(null));
            return mainJSON;
        }
        return null;
    }

}