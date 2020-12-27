package syncserver.events;

import org.json.JSONObject;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

import java.util.Optional;

@SyncServerEvent(event = "CUSTOM_EMOJI")
public class OnRequestCustomEmoji implements SyncServerFunction {

    @Override
    public JSONObject apply(String socketId, JSONObject jsonObject) {
        int clusterId = Integer.parseInt(socketId.split("_")[1]);
        long emojiId = jsonObject.getLong("emoji_id");
        JSONObject responseJson = new JSONObject();
        ClusterConnectionManager.getInstance().getActiveClusters().forEach(cluster -> {
            if (cluster.getClusterId() != clusterId)
                cluster.fetchCustomEmojiTagById(emojiId).ifPresent(emojiTag -> responseJson.put("tag", emojiTag));
        });
        return responseJson;
    }

}