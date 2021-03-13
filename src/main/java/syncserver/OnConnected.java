package syncserver;

import java.util.function.BiFunction;
import org.java_websocket.handshake.ClientHandshake;

public class OnConnected implements BiFunction<String, ClientHandshake, Boolean> {

    @Override
    public Boolean apply(String socketId, ClientHandshake clientHandshake) {
        String[] parts = socketId.split("_");
        String type = parts[0];

        if (type.equals(ClientTypes.CLUSTER)) {
            String id = parts[1];
            onTypeCluster(id, clientHandshake);
        }

        return false;
    }

    private void onTypeCluster(String clusterId, ClientHandshake clientHandshake) {
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
    }

}
