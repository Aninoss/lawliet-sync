package syncserver;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public class SendEvent {

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
        return ClusterConnectionManager.getCluster(clusterId).send("CUSTOM_EMOJI", Map.of("emoji_id", emojiId))
                .thenApply(responseJson -> responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.empty());
    }

    public static CompletableFuture<Optional<String>> sendRequestServerName(int clusterId, long serverId) {
        return ClusterConnectionManager.getCluster(clusterId).send("SERVER_NAME", Map.of("server_id", serverId))
                .thenApply(responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.empty());
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

}
