package core.payments.paddle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class PaddleAPI {

    private static final String USER_AGENT = "Lawliet Discord Bot made by Aninoss#7220";

    private static final OkHttpClient client;

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(25);
        ConnectionPool connectionPool = new ConnectionPool(5, 10, TimeUnit.SECONDS);
        client = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .cache(null)
                .build();
    }

    public static List<JSONObject> retrieveSubscriptions() throws IOException {
        ArrayList<JSONObject> subs = new ArrayList<>();
        JSONArray array = null;
        for(int page = 1; array == null || array.length() >= 200; page++) {
            array = retrieveSubscriptions(page).getJSONArray("response");
            for (int i = 0; i < array.length(); i++) {
                subs.add(array.getJSONObject(i));
            }
        }
        return Collections.unmodifiableList(subs);
    }

    private static JSONObject retrieveSubscriptions(int page) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("vendor_id", System.getenv("PADDLE_VENDOR_ID"))
                .add("vendor_auth_code", System.getenv("PADDLE_AUTH"))
                .add("results_per_page", "200")
                .add("page", String.valueOf(page))
                .build();

        Request request = new Request.Builder()
                .url("https://vendors.paddle.com/api/2.0/subscription/users")
                .post(formBody)
                .addHeader("User-Agent", USER_AGENT)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return new JSONObject(response.body().string());
        }
    }

}
