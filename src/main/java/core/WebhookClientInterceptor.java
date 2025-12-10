package core;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class WebhookClientInterceptor implements Interceptor {

    public static final OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(new WebhookClientInterceptor()).build();

    public static WebhookClient withUrl(String url) {
        return new WebhookClientBuilder(url)
                .setHttpClient(httpClient)
                .build();
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        request = request.newBuilder()
                .url(request.url().url().toString().replace("https://discord.com", System.getenv("NIRN_PROXY_URL")))
                .build();

        return chain.proceed(request);
    }

}
