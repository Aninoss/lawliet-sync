package syncserver;

import java.util.function.Function;

public class OnDisconnected implements Function<String, Boolean> {

    @Override
    public Boolean apply(String socketId) {
        if (socketId != null) {
            String[] parts = socketId.split("_");
            String type = parts[0];

            if (type.equals(ClientTypes.CLUSTER)) {
                String id = parts[1];
                ClusterConnectionManager.getInstance().unregister(Integer.parseInt(id));
            }
        }
        return false;
    }

}
