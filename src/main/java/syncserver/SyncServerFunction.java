package syncserver;

import org.json.JSONObject;

public interface SyncServerFunction {

    JSONObject apply(int clusterId, JSONObject jsonObject);

}