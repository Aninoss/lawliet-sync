package mysql.modules.featurerequests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import mysql.DBMain;

public class DBFeatureRequests {

    public static ArrayList<FREntryBean> fetchEntries(long userId, FRPanelType type) {
        ArrayList<FREntryBean> list = new ArrayList<>();

        String sql = "SELECT id, public, title, description, COUNT(`boostDatetime`) AS `boosts`, date, SUM(IFNULL(`boostDatetime` >= NOW() - INTERVAL 1 WEEK, 0)) AS `recentBoosts`\n" +
                "FROM FeatureRequests\n" +
                "LEFT JOIN FeatureRequestBoosts USING (`id`)\n" +
                "WHERE `type` = ? AND (`public` = 1 OR `userId` = ?)\n" +
                "GROUP BY `id`;";

        try {

            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setString(1, type.name());
            preparedStatement.setLong(2, userId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            while (resultSet.next()) {
                list.add(new FREntryBean(
                        resultSet.getInt(1),
                        resultSet.getBoolean(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getInt(5),
                        resultSet.getDate(6).toLocalDate(),
                        resultSet.getInt(7)
                ));
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public static int fetchBoostsThisWeek(long userId) {
        int ret = -1;

        String sql = "SELECT COUNT(*) FROM FeatureRequestBoosts\n" +
                "WHERE boostUserId = ? AND CONCAT(YEAR(boostDatetime), \"/\", WEEK(boostDatetime)) = CONCAT(YEAR(NOW()), \"/\", WEEK(NOW()));";

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            if (resultSet.next()) {
                ret = resultSet.getInt(1);
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    public static void insertBoost(int id, long userId) {
        DBMain.getInstance().asyncUpdate("INSERT INTO FeatureRequestBoosts(id, boostDatetime, boostUserId) VALUES (?, NOW(), ?);", preparedStatement -> {
            preparedStatement.setInt(1, id);
            preparedStatement.setLong(2, userId);
        });
    }

    public static void postFeatureRequest(long userId, String title, String desc) {
        DBMain.getInstance().asyncUpdate("INSERT INTO FeatureRequests(userId, date, type, public, title, description) VALUES (?, CURDATE(), ?, 0, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, FRPanelType.PENDING.name());
            preparedStatement.setString(3, title);
            preparedStatement.setString(4, desc);
        });
    }

    public static boolean canPost(long userId) {
        boolean canPost = false;
        String sql = "SELECT COUNT(*) FROM FeatureRequests WHERE userId = ? AND public = 0 AND type = ?;";

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, FRPanelType.PENDING.name());
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            if (resultSet.next()) {
                canPost = resultSet.getInt(1) == 0;
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return canPost;
    }

}
