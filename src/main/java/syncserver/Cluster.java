package syncserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.Program;
import okhttp3.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Cluster {

    public enum ConnectionStatus { OFFLINE, BOOTING_UP, FULLY_CONNECTED }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final int clusterId;
    private final String ip;
    private Set<Long> serverIds = Collections.emptySet();
    private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
    private Long localServerSize = null;
    int connectedShards = -1;

    private final LoadingCache<Long, Optional<String>> emojiCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Optional<String> load(@NonNull Long emojiId) throws Exception {
                    return ClusterConnectionManager.getCluster(clusterId)
                            .send(EventOut.CUSTOM_EMOJI, Map.of("emoji_id", emojiId))
                            .thenApply(responseJson -> responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.ofNullable((String) null))
                            .get();
                }
            });

    private final LoadingCache<Long, Optional<String>> serverNameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Optional<String> load(@NonNull Long serverId) throws Exception {
                    return ClusterConnectionManager.getCluster(clusterId)
                            .send(EventOut.SERVER_NAME, Map.of("server_id", serverId))
                            .thenApply(responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.ofNullable((String) null))
                            .get();
                }
            });

    public Cluster(int clusterId, String ip) {
        this.clusterId = clusterId;
        this.ip = ip;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public int getClusterId() {
        return clusterId;
    }

    public boolean isPublicCluster() {
        return clusterId >= 0;
    }

    public String getIp() {
        return ip;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public Set<Long> getServerIds() {
        return serverIds;
    }

    public void setServerIds(Set<Long> serverIds) {
        this.serverIds = serverIds;
    }

    public int getShardIntervalMin() {
        if (Program.isProductionMode() && isPublicCluster()) {
            return (clusterId - 1) * ClusterConnectionManager.getShardsPerCluster();
        } else {
            return 0;
        }
    }

    public int getShardIntervalMax() {
        if (Program.isProductionMode() && isPublicCluster()) {
            int shardsPerCluster = ClusterConnectionManager.getShardsPerCluster();
            return (clusterId - 1) * shardsPerCluster + (shardsPerCluster - 1);
        } else {
            return 0;
        }
    }

    public Optional<Long> getLocalServerSize() {
        return Optional.ofNullable(localServerSize);
    }

    public void setLocalServerSize(Long localServerSize) {
        this.localServerSize = localServerSize;
    }

    public int getConnectedShards() {
        return connectedShards;
    }

    public boolean getAllShardsConnected() {
        return connectedShards == ClusterConnectionManager.getShardsPerCluster();
    }

    public void setConnectedShards(int connectedShards) {
        this.connectedShards = connectedShards;
    }

    public Optional<String> fetchCustomEmojiTagById(long emojiId) {
        if (connectionStatus != ConnectionStatus.FULLY_CONNECTED) {
            return Optional.empty();
        }

        try {
            return emojiCache.get(emojiId);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    public Optional<String> fetchServerNameById(long serverId) {
        if (connectionStatus != ConnectionStatus.FULLY_CONNECTED) {
            return Optional.empty();
        }

        try {
            return serverNameCache.get(serverId);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    public CompletableFuture<JSONObject> send(EventOut eventOut) {
        return send(eventOut, new JSONObject());
    }

    public CompletableFuture<JSONObject> send(EventOut eventOut, Map<String, Object> requestMap) {
        JSONObject requestJson = new JSONObject();
        requestMap.forEach(requestJson::put);
        return send(eventOut, requestJson);
    }

    public CompletableFuture<JSONObject> send(EventOut eventOut, JSONObject requestJson) {
        return send(eventOut.name(), requestJson);
    }

    public CompletableFuture<JSONObject> send(String eventOut, JSONObject requestJson) {
        String url = "http://" + ip + ":" + System.getenv("SYNC_CLIENT_PORT") + "/api/" + eventOut;
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestJson.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", System.getenv("SYNC_AUTH"))
                .build();

        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    JSONObject responseJson = new JSONObject(responseBody.string());
                    future.complete(responseJson);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

}
