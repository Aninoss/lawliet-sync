package syncserver.events;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import core.cache.PatreonCache;
import core.cache.SingleCache;
import mysql.modules.premium.DBPremium;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.*;

@SyncServerEvent(event = "PREMIUM")
public class OnPremium implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(SingleCache.class);

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        if (socketId.equals(ClientTypes.WEB)) {
            JSONObject jsonResponse = new JSONObject();
            JSONArray jsonSlots = new JSONArray();
            JSONArray jsonGuilds = new JSONArray();

            long userId = jsonObject.getLong("user_id");
            int n = getAmountOfSlots(userId);

            if (n > 0) {
                jsonGuilds = getMutualGuilds(userId);
            }

            HashMap<Integer, Long> slotMap = DBPremium.fetchForUser(userId);
            for (int i = 0; i < n; i++) {
                jsonSlots.put(getSlot(slotMap, i));
            }

            jsonResponse.put("guilds", jsonGuilds);
            jsonResponse.put("slots", jsonSlots);

            return jsonResponse;
        }
        return null;
    }

    private JSONArray getMutualGuilds(long userId) {
        JSONArray jsonGuilds = new JSONArray();
        JSONObject jsonUserId = new JSONObject();
        jsonUserId.put("user_id", userId);

        ClusterConnectionManager.getInstance().getActiveClusters().forEach(cluster -> {
            try {
                JSONArray jsonClusterGuilds = SendEvent.sendJSON("MUTUAL_SERVERS", cluster.getClusterId(), jsonUserId).get()
                        .getJSONArray("guilds");

                for (int i = 0; i < jsonClusterGuilds.length(); i++) {
                    jsonGuilds.put(jsonClusterGuilds.getJSONObject(i));
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Exceptions on mutual servers", e);
            }
        });

        return jsonGuilds;
    }

    private long getSlot(HashMap<Integer, Long> slotMap, int i) {
        if (slotMap.containsKey(i)) {
            return slotMap.get(i);
        }
        return 0;
    }

    private int getAmountOfSlots(long userId) {
        int patreonTier = PatreonCache.getInstance().getUserTier(userId);
        return switch (patreonTier) {
            case 3 -> 1;
            case 4 -> 2;
            case 5 -> 5;
            case 6 -> 10;
            default -> 0;
        };
    }

}