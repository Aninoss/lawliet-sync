package syncserver.events;

import java.util.concurrent.CompletableFuture;
import core.ExceptionLogger;
import core.payments.PatreonCache;
import core.payments.PremiumManager;
import org.json.JSONObject;
import syncserver.*;

@SyncServerEvent(event = "PATREON_FETCH")
public class OnPatreonFetch implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        CompletableFuture.supplyAsync(() -> PatreonCache.getInstance().fetch())
                .thenAccept(patreonMap -> {
                    JSONObject jsonPremiumObject = PremiumManager.retrieveJsonData();
                    ClusterConnectionManager.getClusters()
                            .forEach(c -> c.send(EventOut.PATREON, jsonPremiumObject).exceptionally(ExceptionLogger.get()));
                });
        return null;
    }

}