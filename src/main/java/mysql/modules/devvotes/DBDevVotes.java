package mysql.modules.devvotes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import mysql.DBMain;

public class DBDevVotes {

    public static boolean reminderIsActive(long userId) {
        String sql = """
                     SELECT active
                     FROM DevVotesReminders
                     WHERE userId = ?;
                     """;

        boolean active = true;
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                active = resultSet.getBoolean(1);
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return active;
    }

    public static List<String> getUserVotes(long userId, int year, int month) {
        String sql = """
                     SELECT selected
                     FROM DevVotesUservotes
                     WHERE userId = ? AND year = ? AND month = ?;
                     """;

        ArrayList<String> userVotes = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                userVotes.add(resultSet.getString(1));
            }

            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return userVotes;
    }

    public static void updateReminder(long userId, boolean active) throws SQLException, InterruptedException {
        DBMain.getInstance().update("REPLACE INTO DevVotesReminders (userId, active) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
            preparedStatement.setBoolean(2, active);
        });
    }

    public static void updateVotes(long userId, int year, int month, List<String> votes) throws SQLException, InterruptedException {
        DBMain.getInstance().update("DELETE FROM DevVotesUservotes WHERE userId = ? AND year = ? AND month = ?;", preparedStatement -> {
            preparedStatement.setLong(1, userId);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);
        });

        for (String vote : votes) {
            DBMain.getInstance().update("INSERT IGNORE INTO DevVotesUservotes (userId, year, month, selected) VALUES (?, ?, ?, ?);", preparedStatement -> {
                preparedStatement.setLong(1, userId);
                preparedStatement.setInt(2, year);
                preparedStatement.setInt(3, month);
                preparedStatement.setString(4, vote);
            });
        }
    }

}
