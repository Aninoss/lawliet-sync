package core;

import com.stripe.Stripe;
import core.payments.PatreonCache;
import core.payments.paddle.PaddleCache;
import core.payments.stripe.StripeCache;
import hibernate.HibernateManager;
import mysql.DBMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import syncserver.EventManager;
import syncserver.HeartbeatSender;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Stripe.apiKey = System.getenv("STRIPE_API_KEY");
            Program.init();
            Console.getInstance().start();
            DBMain.getInstance().connect();
            HibernateManager.connect();
            PaddleCache.startScheduler();
            StripeCache.startScheduler();
            PatreonCache.getInstance().fetch();
            EventManager.register();
            HealthScheduler.run();
            HeartbeatSender.start();
        } catch (Throwable e) {
            LOGGER.error("Error on startup", e);
            System.exit(1);
        }
    }

}
