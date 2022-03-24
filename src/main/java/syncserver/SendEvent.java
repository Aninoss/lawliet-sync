package syncserver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.json.JSONObject;

public class SendEvent {

    private SendEvent() {
    }

    public static CompletableFuture<JSONObject> sendExit(int clusterId) {
        return ClusterConnectionManager.getCluster(clusterId).send("EXIT", new JSONObject());
    }

    public static CompletableFuture<JSONObject> sendStartConnection(int clusterId, int shardMin, int shardMax, int totalShards) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("shard_min", shardMin);
        dataJson.put("shard_max", shardMax);
        dataJson.put("total_shards", totalShards);

        return ClusterConnectionManager.getCluster(clusterId).send("START_CONNECTION", dataJson);
    }

    public static CompletableFuture<Optional<String>> sendRequestCustomEmoji(int clusterId, long emojiId) {
        return process(
                clusterId,
                "CUSTOM_EMOJI",
                Map.of("emoji_id", emojiId),
                responseJson -> responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.empty()
        );
    }

    public static CompletableFuture<Optional<String>> sendRequestServerName(int clusterId, long serverId) {
        return process(
                clusterId,
                "SERVER_NAME",
                Map.of("server_id", serverId),
                responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.empty()
        );
    }

    public static CompletableFuture<JSONObject> sendUserNotification(int clusterId, long userId, String title, String description, String author, String thumbnail, String image, String footer) {
        return sendUserNotification(clusterId, userId, title, description, author, thumbnail, image, footer, 0);
    }

    public static CompletableFuture<JSONObject> sendUserNotification(int clusterId, long userId, String title, String description, String author, String thumbnail, String image, String footer, int delay) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("user_id", userId)
                .put("title", title)
                .put("description", description)
                .put("author", author)
                .put("thumbnail", thumbnail)
                .put("image", image)
                .put("footer", footer)
                .put("delay", delay);

        return ClusterConnectionManager.getCluster(clusterId).send("NOTIFY", dataJson);
    }

    public static CompletableFuture<JSONObject> sendBlockShards(int clusterId, int totalShards, int shardsMin, int shardsMax) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("total_shards", totalShards);
        dataJson.put("shards_min", shardsMin);
        dataJson.put("shards_max", shardsMax);
        return ClusterConnectionManager.getCluster(clusterId).send("BLOCK_SHARDS", dataJson);
    }

    public static CompletableFuture<JSONObject> sendCmd(int clusterId, String input) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("input", input);
        return ClusterConnectionManager.getCluster(clusterId).send("CMD", dataJson);
    }

    public static CompletableFuture<JSONObject> sendReport(int clusterId, String url, String text, int ipHash) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("url", url);
        dataJson.put("text", text);
        dataJson.put("ip_hash", ipHash);
        return ClusterConnectionManager.getCluster(clusterId).send("REPORT", dataJson);
    }

    public static CompletableFuture<JSONObject> sendEmpty(String event, int clusterId) {
        return ClusterConnectionManager.getCluster(clusterId).send(event, new JSONObject());
    }

    public static CompletableFuture<JSONObject> sendJSON(String event, int clusterId, JSONObject jsonObject) {
        return ClusterConnectionManager.getCluster(clusterId).send(event, jsonObject);
    }

    private static <T> CompletableFuture<T> process(int clusterId, String event, Map<String, Object> jsonMap, Function<JSONObject, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();

        JSONObject dataJson = new JSONObject();
        jsonMap.keySet().forEach(k -> dataJson.put(k, jsonMap.get(k)));
        ClusterConnectionManager.getCluster(clusterId).send(event, dataJson)
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
