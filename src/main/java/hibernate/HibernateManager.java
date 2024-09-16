package hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

public class HibernateManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(HibernateManager.class);

    private static final HashMap<Database, EntityManagerFactory> entityManagerFactories = new HashMap<>();

    public static void connect() {
        for (Database database : Database.values()) {
            Properties props = new Properties();
            props.put("hibernate.ogm.datastore.host", System.getenv("MONGODB_HOST"));
            props.put("hibernate.ogm.datastore.username", System.getenv("MONGODB_USER"));
            props.put("hibernate.ogm.datastore.password",  System.getenv("MONGODB_PASSWORD"));
            props.put("hibernate.ogm.datastore.database",  database.getInternalName());
            LOGGER.info("Connecting with MongoDB database {}", database.getInternalName());
            entityManagerFactories.put(database, Persistence.createEntityManagerFactory("lawliet", props));
        }
    }

    public static EntityManager creasteEntityManager(Database database) {
        return entityManagerFactories.get(database).createEntityManager();
    }

    public static void run(Database database, Consumer<EntityManager> consumer) {
        EntityManager entityManager = creasteEntityManager(database);
        try {
            consumer.accept(entityManager);
        } finally {
            entityManager.close();
        }
    }

    public static <T> T apply(Database database, Function<EntityManager, T> function) {
        EntityManager entityManager = creasteEntityManager(database);
        try {
            return function.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

}
