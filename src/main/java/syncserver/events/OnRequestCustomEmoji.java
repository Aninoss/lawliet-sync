package syncserver.events;

import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "CUSTOM_EMOJI")
public class OnRequestCustomEmoji implements SyncServerFunction {

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        long emojiId = jsonObject.getLong("emoji_id");
        JSONObject responseJson = new JSONObject();
        ClusterConnectionManager.getFullyConnectedClusters().forEach(cluster -> {
            if (cluster.getClusterId() != clusterId) {
                cluster.fetchCustomEmojiTagById(emojiId).ifPresent(emojiTag -> responseJson.put("tag", emojiTag));
            }
        });
        return responseJson;
    }

}