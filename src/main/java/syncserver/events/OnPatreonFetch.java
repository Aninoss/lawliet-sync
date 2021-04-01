package syncserver.events;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import core.cache.PatreonCache;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "PATREON_FETCH")
public class OnPatreonFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            CompletableFuture.supplyAsync(() -> PatreonCache.getInstance().fetch())
                    .thenAccept(patreonMap -> {
                        HashMap<Long, Integer> userTierMap = PatreonCache.getInstance().getUserTiersMap(patreonMap);
                        JSONObject jsonObject = PatreonCache.jsonFromUserUserTiersMap(userTierMap);
                        ClusterConnectionManager.getInstance().getActiveClusters()
                                .forEach(c -> SendEvent.sendJSON(
                                        "PATREON",
                                        c.getClusterId(),
                                        jsonObject
                                ));
                    });
        }
        return null;
    }

}