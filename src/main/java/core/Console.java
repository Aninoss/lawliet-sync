package core;

import core.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.ClusterConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Console {

    private final static Logger LOGGER = LoggerFactory.getLogger(Console.class);

    private static final Console instance = new Console();

    public static Console getInstance() {
        return instance;
    }

    private Console() {
        registerTasks();
    }

    private boolean started = false;
    private final HashMap<String, ConsoleTask> tasks = new HashMap<>();

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(this::manageConsole, "console", 1).start();
    }

    private void registerTasks() {
        tasks.put("help", this::onHelp);

        tasks.put("quit", this::onQuit);
        tasks.put("threads", this::onThreads);
        tasks.put("threads_stop", this::onThreadStop);
        tasks.put("backup", this::onBackup);
        tasks.put("start", this::onStart);
        tasks.put("clusters", this::onClusters);
    }

    private void onClusters(String[] args) {
        ClusterConnectionManager.getInstance().getClusters().forEach(cluster -> {
            LOGGER.info("Cluster {}: {}", cluster.getClusterId(), cluster.getConnectionStatus().toString());
        });
    }

    private void onStart(String[] args) {
        ClusterConnectionManager.getInstance().start();
    }

    private void onBackup(String[] args) {
        SystemUtil.backupDB();
        System.out.println("Backup completed!");
    }

    private void onThreadStop(String[] args) {
        int stopped = 0;

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                t.interrupt();
                stopped++;
            }
        }

        LOGGER.info("{} thread/s interrupted", stopped);
    }

    private void onThreads(String[] args) {
        StringBuilder sb = new StringBuilder();

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                sb.append(t.getName()).append(", ");
            }
        }

        String str = sb.toString();
        if (str.length() >= 2) str = str.substring(0, str.length() - 2);

        LOGGER.info("\n--- THREADS ({}) ---\n{}\n", Thread.getAllStackTraces().size(), str);
    }

    private void onQuit(String[] args) {
        LOGGER.info("EXIT - User commanded exit");
        SystemUtil.backupDB();
        System.exit(0);
    }

    private void onHelp(String[] args) {
        tasks.keySet().stream()
                .filter(key -> !key.equals("help"))
                .sorted()
                .forEach(key -> System.out.println("- " + key));
    }

    private void manageConsole() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                if (br.ready()) {
                    String[] args = br.readLine().split(" ");
                    ConsoleTask task = tasks.get(args[0]);
                    if (task != null) {
                        new CustomThread(() -> {
                            try {
                                task.process(args);
                            } catch (Throwable throwable) {
                                LOGGER.error("Console task {} endet with exception", args[0], throwable);
                            }
                        }, "console_task", 1).start();
                    } else {
                        System.err.printf("No result for \"%s\"\n", args[0]);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Unexpected console exception", e);
            }
        }
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}