package mysql.modules.paddlesubscriptions;

import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBMain;

public class DBPaddleSubscriptions {

    public static Map<Integer, PaddleData> retrievePaddleSubscriptionMap() {
        return new DBDataLoad<PaddleData>("PaddleSubscriptions", "subId, userId, unlocksServer", "1")
                .getHashMap(
                        PaddleData::getSubId,
                        resultSet -> new PaddleData(
                                resultSet.getInt(1),
                                resultSet.getLong(2),
                                resultSet.getBoolean(3)
                        )
                );
    }

    public static void savePaddleSubscription(int subId, long userId, boolean unlocksServer) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO PaddleSubscriptions(subId, userId, unlocksServer) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, subId);
            preparedStatement.setLong(2, userId);
            preparedStatement.setBoolean(3, unlocksServer);
        });
    }

}
