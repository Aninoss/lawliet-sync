package core.util;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(SystemUtil.class);

    private SystemUtil() {
    }

    public static int executeProcess(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process p = pb.start();
            p.waitFor();
            return p.exitValue();
        } catch (Throwable e) {
            LOGGER.error("Could not run process", e);
        }

        return -1;
    }

    public static void backupDB() {
        String filename = LocalDateTime.now().toString();
        SystemUtil.executeProcess("./backupdb.sh", filename);
        LOGGER.info("Database backup completed!");
    }

}
