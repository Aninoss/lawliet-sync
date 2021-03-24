package mysql.modules.patreon;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleBeanGenerator;

public class DBPatreon extends DBSingleBeanGenerator<HashMap<Long, PatreonBean>> {

    private static final DBPatreon ourInstance = new DBPatreon();

    public static DBPatreon getInstance() {
        return ourInstance;
    }

    private DBPatreon() {
    }

    @Override
    protected HashMap<Long, PatreonBean> loadBean() {
        return new DBDataLoad<PatreonBean>("Patreon", "userId, tier, expires", "1")
                .getHashMap(
                        PatreonBean::getUserId,
                        resultSet -> new PatreonBean(
                                resultSet.getLong(1),
                                resultSet.getInt(2),
                                resultSet.getDate(3).toLocalDate()
                        )
                );
    }

    @Override
    public Integer getExpirationTimeMinutes() {
        return 5;
    }

    public static ArrayList<Long> retrieveOldUsers() {
        return new DBDataLoad<Long>("PatreonOld", "userId", "1")
                .getArrayList(r -> r.getLong(1));
    }

    public static void transferToNewSystem(long userId) {
        String sql = """
                     DELETE FROM PatreonOld
                     WHERE userId = ?;
                     """;

        try {
            DBMain.getInstance().update(sql, preparedStatement -> {
                preparedStatement.setLong(1, userId);
            });
        } catch (SQLException | InterruptedException sqlException) {
            throw new RuntimeException(sqlException);
        }
    }

}
