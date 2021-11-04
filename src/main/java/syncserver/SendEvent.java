package syncserver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.json.JSONObject;

public class SendEvent {

    private SendEvent() {
    }

    public static CompletableFuture<JSONObject> sendExit(long clusterId) {
        return SyncManager.getInstance().getServer().sendSecure(ClientTypes.CLUSTER + "_" + clusterId, "EXIT", new JSONObject());
    }

    public static CompletableFuture<JSONObject> sendStartConnection(long clusterId, int shardMin, int shardMax, int totalShards) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("shard_min", shardMin);
        dataJson.put("shard_max", shardMax);
        dataJson.put("total_shards", totalShards);

        return SyncManager.getInstance().getServer().sendSecure(ClientTypes.CLUSTER + "_" + clusterId, "START_CONNECTION", dataJson);
    }

    public static CompletableFuture<Optional<String>> sendRequestCustomEmoji(long clusterId, long emojiId) {
        return process(
                clusterId,
                "CUSTOM_EMOJI",
                Map.of("emoji_id", emojiId),
                responseJson -> responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.empty()
        );
    }

    public static CompletableFuture<Optional<String>> sendRequestServerName(long clusterId, long serverId) {
        return process(
                clusterId,
                "SERVER_NAME",
                Map.of("server_id", serverId),
                responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.empty()
        );
    }

    public static CompletableFuture<JSONObject> sendUserNotification(long clusterId, long userId, String title, String description, String author, String thumbnail, String image, String footer) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("user_id", userId)
                .put("title", title)
                .put("description", description)
                .put("author", author)
                .put("thumbnail", thumbnail)
                .put("image", image)
                .put("footer", footer);

        return SyncManager.getInstance().getServer().sendSecure(ClientTypes.CLUSTER + "_" + clusterId, "NOTIFY", dataJson);
    }

    public static CompletableFuture<JSONObject> sendBlockShards(long clusterId, int totalShards, int shardsMin, int shardsMax) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("total_shards", totalShards);
        dataJson.put("shards_min", shardsMin);
        dataJson.put("shards_max", shardsMax);
        return SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, "BLOCK_SHARDS", dataJson);
    }

    public static CompletableFuture<JSONObject> sendCmd(long clusterId, String input) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("input", input);
        return SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, "CMD", dataJson);
    }

    public static CompletableFuture<JSONObject> sendReport(long clusterId, String url, String text, int ipHash) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("url", url);
        dataJson.put("text", text);
        dataJson.put("ip_hash", ipHash);
        return SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, "REPORT", dataJson);
    }

    public static CompletableFuture<JSONObject> sendEmpty(String event, long clusterId) {
        return SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, event, new JSONObject());
    }

    public static CompletableFuture<JSONObject> sendJSON(String event, long clusterId, JSONObject jsonObject) {
        return SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, event, jsonObject);
    }

    public static CompletableFuture<JSONObject> sendJSONSecure(String event, long clusterId, JSONObject jsonObject) {
        return SyncManager.getInstance().getServer().sendSecure(ClientTypes.CLUSTER + "_" + clusterId, event, jsonObject);
    }

    private static <T> CompletableFuture<T> process(long clusterId, String event, Map<String, Object> jsonMap, Function<JSONObject, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();

        JSONObject dataJson = new JSONObject();
        jsonMap.keySet().forEach(k -> dataJson.put(k, jsonMap.get(k)));
        SyncManager.getInstance().getServer().send(ClientTypes.CLUSTER + "_" + clusterId, event, dataJson)
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                })
                .thenAccept(jsonResponse -> {
                    T t = function.apply(jsonResponse);
                    future.complete(t);
                });

        return future;
    }

}
