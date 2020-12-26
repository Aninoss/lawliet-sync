import core.CustomWebSocketClient;
import org.json.JSONObject;
import syncserver.SyncManager;

import java.net.URISyntaxException;

public class Main {

    public static void main(String[] args) throws InterruptedException, URISyntaxException {
        SyncManager.getInstance().start();
    }

}
