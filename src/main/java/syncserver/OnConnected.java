package syncserver;

import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class OnConnected implements BiFunction<String, ClientHandshake, Boolean> {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnConnected.class);

    @Override
    public Boolean apply(String socketId, ClientHandshake clientHandshake) {
        String[] parts = socketId.split("_");
        String type = parts[0];
        String id = parts[1];
        switch (type) {
            case ClientTypes.CLUSTER:
                onTypeCluster(id, clientHandshake);
                break;

            case ClientTypes.WEB:
                onTypeWeb(clientHandshake);
                break;

            default:
                LOGGER.error("Unknown client type: " + type);
        }

        return false;
    }

    private void onTypeWeb(ClientHandshake clientHandshake) {
        //TODO
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
