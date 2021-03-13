package mysql.modules.patreon;

import java.util.HashMap;
import mysql.DBDataLoad;
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

}
