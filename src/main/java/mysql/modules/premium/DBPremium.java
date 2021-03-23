package mysql.modules.premium;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import mysql.DBMain;
import syncserver.ClusterConnectionManager;

public class DBPremium {

    public static HashMap<Long, ArrayList<PremiumSlot>> fetchAll() {
        HashMap<Long, ArrayList<PremiumSlot>> userSlotMap = new HashMap<>();
        String sql = """
                     SELECT userId, slot, serverId
                     FROM Premium;
                     """;

        try {
            Statement statement = DBMain.getInstance().statementExecuted(sql);
            ResultSet resultSet = statement.getResultSet();
            while(resultSet.next()) {
                long userId = resultSet.getLong(1);
                int slot = resultSet.getInt(2);
                long guildId = resultSet.getLong(3);
                userSlotMap.computeIfAbsent(userId, k -> new ArrayList<>())
                        .add(new PremiumSlot(userId, slot, guildId));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return userSlotMap;
    }

    public static HashMap<Integer, Long> fetchForUser(long userId) {
        HashMap<Integer, Long> map = new HashMap<>();

        String sql = """
                     SELECT slot, serverId
                     FROM Premium
                     WHERE userId = ?;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            while (resultSet.next()) {
                int slot = resultSet.getInt(1);
                long serverId = resultSet.getLong(2);
                map.put(slot, serverId);
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    public static boolean canModify(long userId, int i) {
        if (userId == ClusterConnectionManager.OWNER_ID) {
            return true;
        }
        boolean can;

        String sql = """
                     SELECT time
                     FROM Premium
                     WHERE userId = ? AND slot = ?;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, i);
            preparedStatement.execute();
            ResultSet resultSet = preparedStatement.getResultSet();

            if (resultSet.next()) {
                Instant instant = resultSet.getTimestamp(1).toInstant();
                can = instant.plus(7, ChronoUnit.DAYS).isBefore(Instant.now());
            } else {
                can = true;
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return can;
    }

    public static void modify(long userId, int i, long serverId) {
        String sql = """
                     REPLACE INTO Premium(userId, slot, serverId, time)
                     VALUES(?, ?, ?, NOW());
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, i);
            preparedStatement.setLong(3, serverId);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(long userId, int i) {
        String sql = """
                     DELETE FROM Premium
                     WHERE userId = ? AND slot = ?;
                     """;

        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, i);
            preparedStatement.execute();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
