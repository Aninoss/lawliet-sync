package syncserver.events;

import core.payments.PremiumManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.ClientTypes;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "PATREON")
public class OnPatreon implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnPatreon.class);

    @Override
    public JSONObject apply(String socketId, JSONObject dataJson) {
        try {
            if (socketId.startsWith(ClientTypes.CLUSTER)) {
                return PremiumManager.retrieveJsonData();
            }
        } catch (Throwable e) {
            LOGGER.error("Error", e);
        }
        return null;
    }

}