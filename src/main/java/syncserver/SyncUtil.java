package syncserver;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public class SyncUtil {

    public static CompletableFuture<JSONObject> sendUserNotification(Cluster cluster, long userId, String title, String description, String author, String thumbnail, String image, String footer) {
        return sendUserNotification(cluster, userId, title, description, author, thumbnail, image, footer, 0);
    }

    public static CompletableFuture<JSONObject> sendUserNotification(Cluster cluster, long userId, String title, String description, String author, String thumbnail, String image, String footer, int delay) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("user_id", userId)
                .put("title", title)
                .put("description", description)
                .put("author", author)
                .put("thumbnail", thumbnail)
                .put("image", image)
                .put("footer", footer)
                .put("delay", delay);

        return cluster.send(EventOut.NOTIFY, dataJson);
    }

    public static CompletableFuture<JSONObject> sendCmd(Cluster cluster, String input) {
        return cluster.send(EventOut.CMD, Map.of("input", input));
    }

}
