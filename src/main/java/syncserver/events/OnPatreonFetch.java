package syncserver.events;

import core.cache.PatreonCache;
import org.json.JSONArray;
import org.json.JSONObject;
import syncserver.*;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@SyncServerEvent(event = "PATREON_FETCH")
public class OnPatreonFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            CompletableFuture.supplyAsync(() -> PatreonCache.getInstance().fetch())
                    .thenAccept(userTierMap -> {
                        JSONObject jsonObject = PatreonCache.jsonFromUserPatreonMap(userTierMap);
                        ClusterConnectionManager.getInstance().getActiveClusters()
                                .forEach(c -> SendEvent.sendJSON("PATREON",
                                        c.getClusterId(),
                                        jsonObject
                                ));
                    });
        }
        return null;
    }

}