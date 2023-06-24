package syncserver.events;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import core.ExceptionLogger;
import core.util.StringUtil;
import org.json.JSONObject;
import syncserver.ClusterConnectionManager;
import syncserver.SyncServerEvent;
import syncserver.SyncServerFunction;

@SyncServerEvent(event = "EXCEPTION")
public class OnException implements SyncServerFunction {

    private final WebhookClient webhookClient = WebhookClient.withUrl(System.getenv("EXCEPTION_WEBHOOK_URL"));
    private final ArrayList<Instant> cache = new ArrayList<>();
    private final int CACHE_SIZE = Integer.parseInt(System.getenv("EXCEPTION_CACHE_SIZE"));
    private final int CACHE_TIME_MINUTES = Integer.parseInt(System.getenv("EXCEPTION_CACHE_TIME_MINUTES"));
    private final int BLOCK_TIME_MINUTES = Integer.parseInt(System.getenv("EXCEPTION_BLOCK_TIME_MINUTES"));

    private Instant blockUntil = Instant.now();

    @Override
    public JSONObject apply(int clusterId, JSONObject jsonObject) {
        cache.add(Instant.now());
        if (cache.size() > CACHE_SIZE) {
            if (cache.remove(0).plus(Duration.ofMinutes(CACHE_TIME_MINUTES)).isAfter(Instant.now()) &&
                    Instant.now().isAfter(blockUntil)
            ) {
                blockUntil = Instant.now().plus(Duration.ofMinutes(BLOCK_TIME_MINUTES));
                String message = jsonObject.getString("message").replace("\tat ", "- ");
                WebhookEmbed webhookEmbed = new WebhookEmbedBuilder()
                        .setTitle(new WebhookEmbed.EmbedTitle("Exception Alert (Cluster " + clusterId + ")", null))
                        .setDescription(StringUtil.shortenString(message, 1024))
                        .setColor(Color.RED.getRGB())
                        .build();
                WebhookMessage webhookMessage = new WebhookMessageBuilder()
                        .setContent("<@" + ClusterConnectionManager.OWNER_ID + ">")
                        .addEmbeds(webhookEmbed)
                        .build();
                webhookClient.send(webhookMessage)
                        .exceptionally(ExceptionLogger.get());
            }
        }

        return null;
    }

}
