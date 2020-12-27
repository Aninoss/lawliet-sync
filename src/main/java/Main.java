import core.Console;
import core.Program;
import core.cache.PatreonCache;
import mysql.DBMain;
import syncserver.SyncManager;

import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws SQLException {
        boolean production = args.length >= 1 && args[0].equals("production");
        Program.init(production);
        Console.getInstance().start();
        DBMain.getInstance().connect();
        PatreonCache.getInstance().fetch();
        SyncManager.getInstance().start();
    }

}
