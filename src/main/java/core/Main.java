package core;

import core.cache.PatreonCache;
import mysql.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.SyncManager;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Program.init();
            Console.getInstance().start();
            DBMain.getInstance().connect();
            PatreonCache.getInstance().fetch();
            SyncManager.getInstance().start();
        } catch (Throwable e) {
            LOGGER.error("Error on startup", e);
            System.exit(1);
        }
    }

}
