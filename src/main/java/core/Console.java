package core;

import java.time.Instant;
import java.util.HashMap;
import java.util.Scanner;
import core.payments.PatreonCache;
import core.util.SystemUtil;
import mysql.RedisManager;
import mysql.modules.featurerequests.DBFeatureRequests;
import mysql.modules.featurerequests.FREntryData;
import mysql.modules.featurerequests.FRPanelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.Cluster;
import syncserver.ClusterConnectionManager;
import syncserver.SyncUtil;

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
        tasks.put("shards", this::onShards);
        tasks.put("server", this::onServer);
        tasks.put("ratelimit", this::onRatelimit);
        tasks.put("connect", this::onConnect);
        tasks.put("cmd", this::onCmd);
        tasks.put("fr", this::onFeatureRequest);
        tasks.put("patreon_update", this::onPatreonUpdate);
        tasks.put("reports_ban", this::onReportsBan);
        tasks.put("reports_unban", this::onReportsUnban);
    }

    private void onReportsBan(String[] args) {
        String ipHash = args[1];
        RedisManager.update(jedis -> {
            jedis.hset("reports_banned", ipHash, Instant.now().toString());
        });
        LOGGER.info("{} got successfully banned from reports", ipHash);
    }

    private void onReportsUnban(String[] args) {
        String ipHash = args[1];
        RedisManager.update(jedis -> {
            jedis.hdel("reports_banned", ipHash);
        });
        LOGGER.info("{} got successfully unbanned from reports", ipHash);
    }

    private void onPatreonUpdate(String[] args) {
        PatreonCache.getInstance().fetch();
    }

    private void onFeatureRequest(String[] args) throws Exception {
        boolean accept = switch (args[1].toLowerCase()) {
            case "accept" -> true;
            case "deny" -> false;
            default -> throw new NoSuchMethodException("invalid method");
        };

        int id = Integer.parseInt(args[2]);
        FREntryData entryData = DBFeatureRequests.fetchFeatureRequest(Integer.parseInt(args[2]));
        String title;
        String desc;
        if (accept) {
            DBFeatureRequests.updateFeatureRequestStatus(entryData.getId(), FRPanelType.PENDING, true);
            title = String.format("✅ Your feature request \"%s\" has been accepted", entryData.getTitle());
            desc = String.format("You should now [boost](https://lawlietbot.xyz/featurerequests?search=%d) your entry to increase it's exposure!", id);
        } else {
            DBFeatureRequests.updateFeatureRequestStatus(entryData.getId(), FRPanelType.REJECTED, false);
            title = String.format("❌ Unfortunately, your feature request \"%s\" got rejected", entryData.getTitle());
            String content = collectArgs(args, 3);
            desc = content.length() > 0 ? String.format("Reason:```%s```", collectArgs(args, 3)) : null;
        }

        ClusterConnectionManager.getFirstFullyConnectedCluster().ifPresent(cluster -> {
            SyncUtil.sendUserNotification(
                    cluster,
                    entryData.getUserId(),
                    title,
                    desc,
                    null,
                    null,
                    null,
                    null
            ).exceptionally(ExceptionLogger.get());
        });

        LOGGER.info("Feature Request Update Successful");
    }

    private void onCmd(String[] args) {
        int clusterId = Integer.parseInt(args[1]);
        StringBuilder inputBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                inputBuilder.append(" ");
            }
            inputBuilder.append(args[i]);
        }

        if (clusterId >= 1) {
            SyncUtil.sendCmd(ClusterConnectionManager.getCluster(clusterId), inputBuilder.toString())
                    .exceptionally(ExceptionLogger.get());
        } else {
            ClusterConnectionManager.getClusters()
                    .forEach(c -> SyncUtil.sendCmd(c, inputBuilder.toString()).exceptionally(ExceptionLogger.get()));
        }
    }

    private void onConnect(String[] args) {
        int clusterId = Integer.parseInt(args[1]);
        Cluster cluster = ClusterConnectionManager.getCluster(clusterId);
        ClusterConnectionManager.submitConnectCluster(cluster, true, false);
    }

    private void onRatelimit(String[] args) {
        double intervalTimeMillis = Double.parseDouble(args[1]);
        SyncedRatelimitManager.getInstance().setIntervalTimeNanos(Math.round(intervalTimeMillis * 1_000_000));
        LOGGER.info("Synced ratelimit interval set to: {} ms", intervalTimeMillis);
    }

    private void onServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        int cluster = ClusterConnectionManager.getResponsibleCluster(serverId).getClusterId();
        int shard = ClusterConnectionManager.getResponsibleShard(serverId);
        LOGGER.info("Server: {}; Cluster: {}; Shard: {}", serverId, cluster, shard);
    }

    private void onClusters(String[] args) {
        ClusterConnectionManager.getClusters().forEach(cluster -> {
            LOGGER.info(
                    "Cluster {}: {} ({} servers, shard {} - {})",
                    cluster.getClusterId(),
                    cluster.getConnectionStatus().toString(),
                    cluster.getLocalServerSize().orElse(0L),
                    cluster.getShardIntervalMin(),
                    cluster.getShardIntervalMax()
            );
        });
    }

    private void onShards(String[] args) {
        LOGGER.info("Total shards: {}", ClusterConnectionManager.getTotalShards().orElse(0));
    }

    private void onStart(String[] args) {
        ClusterConnectionManager.start();
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
        System.exit(0);
    }

    private void onHelp(String[] args) {
        tasks.keySet().stream()
                .filter(key -> !key.equals("help"))
                .sorted()
                .forEach(key -> System.out.println("- " + key));
    }

    private String collectArgs(String[] args, int firstIndex) {
        StringBuilder argsString = new StringBuilder();
        for (int i = firstIndex; i < args.length; i++) {
            argsString.append(" ").append(args[i]);
        }
        return argsString.toString().trim();
    }

    private void manageConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.length() > 0) {
                    String[] args = line.split(" ");
                    ConsoleTask task = tasks.get(args[0]);
                    if (task != null) {
                        GlobalThreadPool.getExecutorService().submit(() -> {
                            try {
                                task.process(args);
                            } catch (Throwable throwable) {
                                LOGGER.error("Console task {} ended with exception", args[0], throwable);
                            }
                        });
                    } else {
                        System.err.printf("No result for \"%s\"\n", args[0]);
                    }
                }
            }
        }
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}