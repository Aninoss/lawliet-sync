package syncserver.events;

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
            return PatreonCache.jsonFromUserPatreonMap(PatreonCache.getInstance().getUserTiersMap());
        }
        return null;
    }

}