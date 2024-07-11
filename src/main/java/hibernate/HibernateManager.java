package hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Properties;
import java.util.function.Consumer;

public class HibernateManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(HibernateManager.class);

    private static EntityManagerFactory entityManagerFactory;

    public static void connect() {
        Properties props = new Properties();
        props.put("hibernate.ogm.datastore.host", System.getenv("MONGODB_HOST"));
        props.put("hibernate.ogm.datastore.username", System.getenv("MONGODB_USER"));
        props.put("hibernate.ogm.datastore.password",  System.getenv("MONGODB_PASSWORD"));
        LOGGER.info("Connecting with MongoDB database");
        entityManagerFactory = Persistence.createEntityManagerFactory("lawliet", props);
    }

    public static EntityManager creasteEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public static void run(Consumer<EntityManager> consumer) {
        EntityManager entityManager = creasteEntityManager();
        try {
            consumer.accept(entityManager);
        } finally {
            entityManager.close();
        }
    }

}
