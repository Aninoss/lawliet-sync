package mysql.modules.botstats;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import mysql.DBMain;

public class DBBotStats {

    public static List<BotStatsServersSlot> getMonthlyServerStats() {
        ArrayList<BotStatsServersSlot> slots = new ArrayList<>();

        try {
            Statement statement = DBMain.getInstance().statementExecuted("SELECT (MONTH(`date`) - 1) AS mon, YEAR(`date`) AS yea, MAX(`count`), MIN(`date`) AS dat FROM StatsServerCount GROUP BY mon, yea ORDER BY dat DESC LIMIT 13;");
            ResultSet resultSet = statement.getResultSet();

            while (resultSet.next()) {
                BotStatsServersSlot slot = new BotStatsServersSlot(
                        resultSet.getInt(1),
                        resultSet.getInt(2),
                        resultSet.getInt(3)
                );
                slots.add(0, slot);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return slots;
    }

}
