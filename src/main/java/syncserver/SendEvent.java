package syncserver;

import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

public class SendEvent {

    private SendEvent() {}

    public static CompletableFuture<JSONObject> sendExit(long clusterId) {
        return SyncManager.getInstance().getServer().sendSecure(String.valueOf(clusterId), "EXIT", new JSONObject());
    }

    public static CompletableFuture<JSONObject> sendStartConnection(long clusterId, int shardMin, int shardMax, int totalShards) {
        JSONObject dataJson = new JSONObject();
        dataJson.put("shard_min", shardMin);
        dataJson.put("shard_max", shardMax);
        dataJson.put("total_shards", totalShards);

        return SyncManager.getInstance().getServer().sendSecure(String.valueOf(clusterId), "START_CONNECTION", dataJson);
    }

}
