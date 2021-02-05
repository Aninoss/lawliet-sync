package core;

import core.cache.PatreonCache;
import mysql.DBMain;
import syncserver.SyncManager;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws SQLException {
        Program.init();
        Console.getInstance().start();
        DBMain.getInstance().connect();
        PatreonCache.getInstance().fetch();
        SyncManager.getInstance().start();
    }

}
