package mysql.modules.featurerequests;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import mysql.DBMain;

public class DBFeatureRequests {

    public static ArrayList<FREntryData> fetchEntries(long userId, FRPanelType type) {
        ArrayList<FREntryData> list = new ArrayList<>();

        String sql = """
                     SELECT id, userId, public, title, description, COUNT(`boostDatetime`) AS `boosts`, date, SUM(IFNULL(`boostDatetime` >= NOW() - INTERVAL 1 WEEK, 0)) AS `recentBoosts`
                     FROM FeatureRequests
                     LEFT JOIN FeatureRequestBoosts USING (`id`)
                     WHERE `type` = ? AND (`public` = 1 OR `userId` = ?)
                     GROUP BY `id`;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setString(1, type.name());
            preparedStatement.setLong(2, userId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            while (resultSet.next()) {
                list.add(new FREntryData(
                        resultSet.getInt(1),
                        resultSet.getLong(2),
                        resultSet.getBoolean(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getInt(6),
                        resultSet.getDate(7).toLocalDate(),
                        resultSet.getInt(8)
                ));
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public static FREntryData fetchFeatureRequest(int id) {
        FREntryData entry = null;

        String sql = """
                     SELECT id, userId, public, title, description, COUNT(`boostDatetime`) AS `boosts`, date, SUM(IFNULL(`boostDatetime` >= NOW() - INTERVAL 1 WEEK, 0)) AS `recentBoosts`
                     FROM FeatureRequests
                     LEFT JOIN FeatureRequestBoosts USING (`id`)
                     WHERE `id` = ?
                     GROUP BY `id`;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setInt(1, id);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            while (resultSet.next()) {
                entry = new FREntryData(
                        resultSet.getInt(1),
                        resultSet.getLong(2),
                        resultSet.getBoolean(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getInt(6),
                        resultSet.getDate(7).toLocalDate(),
                        resultSet.getInt(8)
                );
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return entry;
    }

    public static int fetchBoostsThisWeek(long userId) {
        int ret = -1;

        String sql = """
                     SELECT COUNT(*) FROM FeatureRequestBoosts
                     WHERE boostUserId = ? AND CONCAT(YEAR(boostDatetime), "/", WEEK(boostDatetime)) = CONCAT(YEAR(NOW()), "/", WEEK(NOW()));
                     """;

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

    public static int getNewestId() {
        int ret = -1;

        String sql = """
                     SELECT id FROM FeatureRequests
                     ORDER BY id DESC LIMIT 1;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
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

    public static void postFeatureRequest(int id, long userId, String title, String desc) {
        DBMain.getInstance().asyncUpdate("INSERT INTO FeatureRequests(id, userId, date, type, public, title, description) VALUES (?, ?, CURDATE(), ?, 0, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, id);
            preparedStatement.setLong(2, userId);
            preparedStatement.setString(3, FRPanelType.PENDING.name());
            preparedStatement.setString(4, title);
            preparedStatement.setString(5, desc);
        });
    }

    public static void updateFeatureRequestStatus(int id, FRPanelType type, boolean pub) {
        DBMain.getInstance().asyncUpdate("UPDATE FeatureRequests SET type = ?, public = ? WHERE id = ?;", preparedStatement -> {
            preparedStatement.setString(1, type.name());
            preparedStatement.setBoolean(2, pub);
            preparedStatement.setInt(3, id);
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
