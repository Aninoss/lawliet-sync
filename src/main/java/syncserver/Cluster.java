package syncserver;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.Program;
import okhttp3.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Cluster {

    public enum ConnectionStatus { OFFLINE, BOOTING_UP, FULLY_CONNECTED }

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final int clusterId;
    private final String ip;
    private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
    private Long localServerSize = null;

    private final LoadingCache<Long, Optional<String>> emojiCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Optional<String> load(@NonNull Long emojiId) throws Exception {
                    return SendEvent.sendRequestCustomEmoji(clusterId, emojiId).get();
                }
            });

    private final LoadingCache<Long, Optional<String>> serverNameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public Optional<String> load(@NonNull Long serverId) throws Exception {
                    return SendEvent.sendRequestServerName(clusterId, serverId).get();
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

    public String getIp() {
        return ip;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public int getShardIntervalMin() {
        if (Program.isProductionMode()) {
            return (clusterId - 1) * 16;
        } else {
            return 0;
        }
    }

    public int getShardIntervalMax() {
        if (Program.isProductionMode()) {
            return (clusterId - 1) * 16 + 15;
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

    public CompletableFuture<JSONObject> send(String name, JSONObject requestJson) {
        String url = "http://" + ip + ":" + System.getenv("SYNC_CLIENT_PORT") + "/api/" + name;
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
