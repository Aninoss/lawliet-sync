package core.cache;

import core.Program;
import org.javacord.api.DiscordApiBuilder;

public class DiscordRecommendedTotalShardsCache extends SingleCache<Integer> {

    private static final DiscordRecommendedTotalShardsCache ourInstance = new DiscordRecommendedTotalShardsCache();

    public static DiscordRecommendedTotalShardsCache getInstance() {
        return ourInstance;
    }

    private DiscordRecommendedTotalShardsCache() {
    }

    @Override
    protected Integer fetchValue() {
        return fetchRecommendedTotalShards();
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 60;
    }

    private int fetchRecommendedTotalShards() {
        if (!Program.isProductionMode()) {
            return 1;
        }

        return new DiscordApiBuilder()
                .setToken(System.getenv("BOT_TOKEN"))
                .setRecommendedTotalShards()
                .join()
                .getTotalShards();
    }

}
