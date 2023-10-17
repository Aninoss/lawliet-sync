package core.payments;

import core.Program;
import core.SingleCache;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import mysql.modules.patreon.DBPatreon;
import mysql.modules.patreon.PatreonBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PatreonCache extends SingleCache<HashMap<Long, Integer>> {

    private final static Logger LOGGER = LoggerFactory.getLogger(PatreonCache.class);
    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {
    }

    private final Map<String, Integer> TIER_MAP = Map.of(
            "6044874", 1,
            "4928466", 2,
            "5074151", 3,
            "5074320", 4,
            "5080986", 5,
            "5080991", 6
    );

    private final Map<Integer, Integer> TIER_MAP_CENTS = Map.of(
            100, 1,
            300, 2,
            500, 3,
            1000, 4,
            2500, 5,
            5000, 6
    );

    @Override
    protected HashMap<Long, Integer> fetchValue() {
        if (Program.isProductionMode()) {
            try {
                HashMap<Long, Integer> userTiers = new HashMap<>();
                fetchFromUrl("https://www.patreon.com/api/oauth2/v2/campaigns/3334056/members?include=user,currently_entitled_tiers&fields%5Bmember%5D=full_name,patron_status,currently_entitled_amount_cents,pledge_cadence&fields%5Buser%5D=social_connections&page%5Bsize%5D=999", userTiers);
                LOGGER.info("Patreon load successful ({})", userTiers.size());

                return userTiers;
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("Could not fetch patreon data", e);
            }
        }
        return null;
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 5;
    }

    private void fetchFromUrl(String url, HashMap<Long, Integer> userTiers) throws ExecutionException, InterruptedException {
        HttpProperty property = new HttpProperty("Authorization", "Bearer " + System.getenv("PATREON_ACCESS"));
        String data = HttpRequest.getData(url, property).get().getContent().get();
        JSONObject rootJson = new JSONObject(data);

        HashMap<String, Integer> patreonTiers = getPatreonTiers(rootJson.getJSONArray("data"));
        addUserTiersToMap(userTiers, rootJson.getJSONArray("included"), patreonTiers);

        if (rootJson.has("links")) {
            JSONObject linksJson = rootJson.getJSONObject("links");
            if (linksJson.has("next") && !linksJson.isNull("next")) {
                fetchFromUrl(linksJson.getString("next"), userTiers);
            }
        }
    }

    private void addUserTiersToMap(HashMap<Long, Integer> userTiers, JSONArray includedJson, HashMap<String, Integer> patreonTiers) {
        for (int i = 0; i < includedJson.length(); i++) {
            JSONObject slotJson = includedJson.getJSONObject(i);
            if (slotJson.getString("type").equals("user")) {
                JSONObject attributesJson = slotJson.getJSONObject("attributes");
                if (attributesJson.has("social_connections")) {
                    JSONObject socialConnectionsJson = attributesJson.getJSONObject("social_connections");
                    String id = slotJson.getString("id");
                    if (patreonTiers.containsKey(id) && !socialConnectionsJson.isNull("discord")) {
                        long discordUserId = Long.parseLong(socialConnectionsJson.getJSONObject("discord").getString("user_id"));
                        Integer tier = patreonTiers.get(id);
                        if (tier != null) {
                            userTiers.put(discordUserId, patreonTiers.get(id));
                        } else {
                            LOGGER.warn("Empty tier for user id {}", discordUserId);
                        }
                    }
                }
            }
        }
    }

    private HashMap<String, Integer> getPatreonTiers(JSONArray dataJson) {
        HashMap<String, Integer> patreonTiers = new HashMap<>();
        for (int i = 0; i < dataJson.length(); i++) {
            JSONObject slotJson = dataJson.getJSONObject(i);
            JSONObject attributesJson = slotJson.getJSONObject("attributes");
            JSONObject relationshipsJson = slotJson.getJSONObject("relationships");

            if (!attributesJson.isNull("patron_status") && attributesJson.getString("patron_status").equals("active_patron")) {
                JSONArray entitledTiers = relationshipsJson.getJSONObject("currently_entitled_tiers").getJSONArray("data");
                String id = relationshipsJson.getJSONObject("user").getJSONObject("data").getString("id");
                if (!entitledTiers.isEmpty()) {
                    String tierId = entitledTiers.getJSONObject(0).getString("id");
                    Integer tier = TIER_MAP.get(tierId);
                    if (tier != null) {
                        patreonTiers.put(id, tier);
                    } else {
                        LOGGER.warn("Empty tier for tier id {}", id);
                    }
                } else {
                    int entitledCents = attributesJson.getInt("currently_entitled_amount_cents");
                    int pledgeCadence = attributesJson.getInt("pledge_cadence");
                    if (entitledCents > 0) {
                        if (pledgeCadence == 12) {
                            entitledCents /= 10.08;
                        }
                        int minDiff = -1;
                        int tier = 0;
                        for (Map.Entry<Integer, Integer> entry : TIER_MAP_CENTS.entrySet()) {
                            int diff = Math.abs(entry.getKey() - entitledCents);
                            if (diff < minDiff || minDiff == -1) {
                                minDiff = diff;
                                tier = entry.getValue();
                                if (minDiff == 0) {
                                    break;
                                }
                            }
                        }
                        patreonTiers.put(id, tier);
                    }
                }
            }
        }
        return patreonTiers;
    }

    public int getUserTier(long userId) {
        if (!Program.isProductionMode()) {
            return 0;
        }

        PatreonBean patreonBean = DBPatreon.getInstance().getBean().get(userId);
        if (patreonBean != null && patreonBean.isValid()) {
            return patreonBean.getTier();
        }

        return getAsync().getOrDefault(userId, 0);
    }

}
