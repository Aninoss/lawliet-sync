import core.Console;
import core.Program;
import core.schedule.MainScheduler;
import syncserver.ClusterConnectionManager;
import syncserver.SyncManager;

import java.time.temporal.ChronoUnit;

public class Main {

    public static void main(String[] args) {
        boolean production = args.length >= 1 && args[0].equals("production");
        Program.init(production);
        Console.getInstance().start();
        SyncManager.getInstance().start();
    }

}
