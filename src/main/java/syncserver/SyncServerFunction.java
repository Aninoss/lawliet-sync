package syncserver;

import java.util.function.BiFunction;
import org.json.JSONObject;

public interface SyncServerFunction extends BiFunction<String, JSONObject, JSONObject> {

}