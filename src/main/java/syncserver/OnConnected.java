package syncserver;

import org.java_websocket.handshake.ClientHandshake;

import java.util.function.BiFunction;

public class OnConnected implements BiFunction<String, ClientHandshake, Boolean> {

    @Override
    public Boolean apply(String clusterId, ClientHandshake clientHandshake) {
        boolean alreadyConnected = Boolean.parseBoolean(clientHandshake.getFieldValue("already_connected"));
        if (alreadyConnected) {
            ClusterConnectionManager.getInstance().registerAlreadyConnected(
                    Integer.parseInt(clusterId),
                    Integer.parseInt(clientHandshake.getFieldValue("size")),
                    Integer.parseInt(clientHandshake.getFieldValue("shard_min")),
                    Integer.parseInt(clientHandshake.getFieldValue("shard_max")),
                    Integer.parseInt(clientHandshake.getFieldValue("total_shards"))
            );
        } else {
            ClusterConnectionManager.getInstance().register(
                    Integer.parseInt(clusterId),
                    Integer.parseInt(clientHandshake.getFieldValue("size"))
            );
        }
        return false;
    }

}
