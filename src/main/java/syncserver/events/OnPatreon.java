package syncserver.events;

import java.util.HashMap;
import core.cache.PatreonCache;
import org.json.JSONObject;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        if (socketId.startsWith(ClientTypes.CLUSTER)) {
            HashMap<Long, Integer> patreonUserMap = PatreonCache.getInstance().getAsync();
            return PatreonCache.jsonFromUserUserTiersMap(PatreonCache.getInstance().getUserTiersMap(patreonUserMap));
        }
        return null;
    }

}