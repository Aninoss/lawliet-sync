package syncserver;

import java.util.function.Function;

public class OnDisconnected implements Function<String, Boolean> {

    @Override
    public Boolean apply(String clusterId) {
        ClusterConnectionManager.getInstance().unregister(Integer.parseInt(clusterId));
        return false;
    }

}
