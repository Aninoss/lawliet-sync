package syncserver;

import java.util.function.Function;

public class OnDisconnected implements Function<String, Boolean> {

    @Override
    public Boolean apply(String socketId) {
        String[] parts = socketId.split("_");
        String type = parts[0];
        String id = parts[1];

        if (type.equals(ClientTypes.CLUSTER)) {
            ClusterConnectionManager.getInstance().unregister(Integer.parseInt(id));
        }
        return false;
    }

}
