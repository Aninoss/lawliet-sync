package core.payments.paddle;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaddleAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleAPI.class);
    private static final String USER_AGENT = "Lawliet Discord Bot made by Aninoss#7220";

    private static final OkHttpClient client;

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(25);
        ConnectionPool connectionPool = new ConnectionPool(5, 10, TimeUnit.SECONDS);
        client = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .cache(null)
                .build();
    }

    public static List<JSONObject> retrieveSubscriptions() throws IOException {
        return retrieveSubscriptions(0);
    }

    public static List<JSONObject> retrieveSubscriptions(long subId) throws IOException {
        ArrayList<JSONObject> subs = new ArrayList<>();
        JSONArray array = null;
        for (int page = 1; array == null || array.length() >= 200; page++) {
            array = retrieveSubscriptions(subId, page, 5).getJSONArray("response");
            for (int i = 0; i < array.length(); i++) {
                subs.add(array.getJSONObject(i));
            }
        }
        return Collections.unmodifiableList(subs);
    }

    public static String generatePayLink(long planId, int quantity, String[] prices, String[] recurringPrices, String passthrough) throws IOException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("vendor_id", System.getenv("PADDLE_VENDOR_ID"))
                .add("vendor_auth_code", System.getenv("PADDLE_AUTH"))
                .add("product_id", String.valueOf(planId))
                .add("quantity", String.valueOf(quantity))
                .add("passthrough", passthrough);

        for (int i = 0; i < prices.length; i++) {
            formBodyBuilder.add("prices[" + i + "]", prices[i])
                    .add("recurring_prices[" + i + "]", recurringPrices[i]);
        }

        RequestBody formBody = formBodyBuilder.build();
        Request request = new Request.Builder()
                .url("https://vendors.paddle.com/api/2.0/product/generate_pay_link")
                .post(formBody)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            return new JSONObject(body)
                    .getJSONObject("response")
                    .getString("url");
        }
    }

    private static JSONObject retrieveSubscriptions(long subId, int page, int counter) throws IOException {
        FormBody.Builder formBodyBuilder = new FormBody.Builder()
                .add("vendor_id", System.getenv("PADDLE_VENDOR_ID"))
                .add("vendor_auth_code", System.getenv("PADDLE_AUTH"))
                .add("results_per_page", "200")
                .add("state", "active,past_due,paused")
                .add("page", String.valueOf(page));
        if (subId > 0) {
            formBodyBuilder.add("subscription_id", String.valueOf(subId));
        }
        RequestBody formBody = formBodyBuilder.build();

        Request request = new Request.Builder()
                .url("https://vendors.paddle.com/api/2.0/subscription/users")
                .post(formBody)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return new JSONObject(response.body().string());
        } catch (SocketTimeoutException e) {
            if (--counter > 0) {
                LOGGER.error("Paddle timeout, retrying...", e);
                return retrieveSubscriptions(subId, page, counter);
            } else {
                throw e;
            }
        }
    }

}
