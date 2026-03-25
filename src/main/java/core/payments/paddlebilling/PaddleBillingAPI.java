package core.payments.paddlebilling;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PaddleBillingAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(PaddleBillingAPI.class);
    private static final String USER_AGENT = "Lawliet Discord Bot by @aninoss";

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

    public static List<JSONObject> retrieveSubscriptions(String subscriptionId) throws IOException {
        String parameters = "?per_page=200";
        if (subscriptionId != null) {
            parameters += "&id=" + subscriptionId;
        }
        String url = "https://" + System.getenv("PADDLE_SUBDOMAIN_PREFIX") + "api.paddle.com/subscriptions" + parameters;

        ArrayList<JSONObject> subs = new ArrayList<>();
        while (url != null) {
            JSONObject responseJson = retrieveSubscriptions(url, 5);
            JSONObject pagination = responseJson.getJSONObject("meta").getJSONObject("pagination");
            extractSubscriptionJson(subs, responseJson);
            url = pagination.getBoolean("has_more") ? pagination.getString("next") : null;
        }
        return Collections.unmodifiableList(subs);
    }

    private static void extractSubscriptionJson(ArrayList<JSONObject> subs, JSONObject responseJson) {
        JSONArray dataJson = responseJson.getJSONArray("data");
        for (int i = 0; i < dataJson.length(); i++) {
            subs.add(dataJson.getJSONObject(i));
        }
    }

    private static JSONObject retrieveSubscriptions(String url, int errorCounter) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Authorization", "Bearer " + System.getenv("PADDLE_BILLING_API_KEY"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            return new JSONObject(response.body().string());
        } catch (SocketTimeoutException e) {
            if (--errorCounter > 0) {
                LOGGER.error("Paddle billing timeout, retrying...", e);
                return retrieveSubscriptions(url, errorCounter);
            } else {
                throw e;
            }
        }
    }

}
