import core.Console;
import core.schedule.MainScheduler;
import syncserver.ClusterConnectionManager;
import syncserver.SyncManager;

import java.time.temporal.ChronoUnit;

public class Main {

    public static void main(String[] args) {
        Console.getInstance().start();
        SyncManager.getInstance().start();
    }

}
