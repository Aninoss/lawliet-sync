package syncserver.events;

import java.util.concurrent.CompletableFuture;
import core.payments.PatreonCache;
import core.payments.PremiumManager;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "PATREON_FETCH")
public class OnPatreonFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            CompletableFuture.supplyAsync(() -> PatreonCache.getInstance().fetch())
                    .thenAccept(patreonMap -> {
                        JSONObject jsonObject = PremiumManager.retrieveJsonData();
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