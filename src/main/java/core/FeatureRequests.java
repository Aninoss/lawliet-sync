package core;

import mysql.modules.featurerequests.DBFeatureRequests;
import mysql.modules.featurerequests.FREntryData;
import mysql.modules.featurerequests.FRPanelType;
import syncserver.ClusterConnectionManager;
import syncserver.SyncUtil;

public class FeatureRequests {

    public static void accept(int id) {
        FREntryData entryData = DBFeatureRequests.fetchFeatureRequest(id);
        DBFeatureRequests.updateFeatureRequestStatus(entryData.getId(), FRPanelType.PENDING, true);

        send(entryData.getUserId(),
                String.format("✅ Your feature request \"%s\" has been accepted", entryData.getTitle()),
                String.format("You should now [boost](https://lawlietbot.xyz/featurerequests?search=%d) your entry to increase it's exposure!", id)
        );
    }

    public static void deny(int id, String reason) {
        FREntryData entryData = DBFeatureRequests.fetchFeatureRequest(id);
        DBFeatureRequests.updateFeatureRequestStatus(entryData.getId(), FRPanelType.REJECTED, false);
        String desc = reason.isBlank()
                ? null
                : String.format("Reason:```%s```", reason);

        send(entryData.getUserId(),
                String.format("❌ Unfortunately, your feature request \"%s\" got rejected", entryData.getTitle()),
                desc
        );
    }

    private static void send(long userId, String title, String desc) {
        ClusterConnectionManager.getFirstFullyConnectedPublicCluster().ifPresent(cluster -> {
            SyncUtil.sendUserNotification(
                    cluster,
                    userId,
                    title,
                    desc,
                    null,
                    null,
                    null,
                    null
            ).exceptionally(ExceptionLogger.get());
        });
    }

}
