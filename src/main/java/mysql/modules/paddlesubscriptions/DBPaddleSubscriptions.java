package mysql.modules.paddlesubscriptions;

import java.sql.SQLException;
import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBMain;

public class DBPaddleSubscriptions {

    public static Map<Long, PaddleData> retrievePaddleSubscriptionMap() {
        return new DBDataLoad<PaddleData>("PaddleSubscriptions", "subId, userId, unlocksServer", "1")
                .getHashMap(
                        PaddleData::getSubId,
                        resultSet -> new PaddleData(
                                resultSet.getLong(1),
                                resultSet.getLong(2),
                                resultSet.getBoolean(3)
                        )
                );
    }

    public static void savePaddleSubscription(long subId, long userId, boolean unlocksServer) throws SQLException, InterruptedException {
        DBMain.getInstance().update("REPLACE INTO PaddleSubscriptions(subId, userId, unlocksServer) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, subId);
            preparedStatement.setLong(2, userId);
            preparedStatement.setBoolean(3, unlocksServer);
        });
    }

}
