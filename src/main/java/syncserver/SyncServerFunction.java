package syncserver;

import org.json.JSONObject;

import java.util.function.BiFunction;

public interface SyncServerFunction extends BiFunction<String, JSONObject, JSONObject> {}