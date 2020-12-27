package syncserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Cluster {

    public enum ConnectionStatus { OFFLINE, BOOTING_UP, FULLY_CONNECTED }

    private final int clusterId;
    private final int size;
    private ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
    private int[] shardInterval = null;
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

    public Cluster(int clusterId, int size) {
        this.clusterId = clusterId;
        this.size = size;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setShardInterval(int[] shardInterval) {
        this.shardInterval = shardInterval;
    }

    public int getClusterId() {
        return clusterId;
    }

    public int getSize() {
        return size;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public int[] getShardInterval() {
        return shardInterval;
    }

    public Optional<Long> getLocalServerSize() {
        return Optional.ofNullable(localServerSize);
    }

    public void setLocalServerSize(Long localServerSize) {
        this.localServerSize = localServerSize;
    }

    public boolean isActive() {
        return getShardInterval() != null;
    }

    public Optional<String> fetchCustomEmojiTagById(long emojiId) {
        if (connectionStatus != ConnectionStatus.FULLY_CONNECTED)
            return Optional.empty();

        try {
            return emojiCache.get(emojiId);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    public Optional<String> fetchServerNameById(long serverId) {
        if (connectionStatus != ConnectionStatus.FULLY_CONNECTED)
            return Optional.empty();

        try {
            return serverNameCache.get(serverId);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

}
