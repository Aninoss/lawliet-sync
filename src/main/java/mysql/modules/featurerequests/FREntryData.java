package mysql.modules.featurerequests;

import java.time.LocalDate;

public class FREntryData {

    private final int id;
    private final long userId;
    private final boolean publicEntry;
    private final String description, title;
    private final int boosts;
    private final int recentBoosts;
    private final LocalDate date;

    public FREntryData(int id, long userId, boolean publicEntry, String title, String description, int boosts, LocalDate date, int recentBoosts) {
        this.id = id;
        this.userId = userId;
        this.publicEntry = publicEntry;
        this.title = title;
        this.description = description;
        this.boosts = boosts;
        this.recentBoosts = recentBoosts;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isPublicEntry() {
        return publicEntry;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getBoosts() {
        return boosts;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getRecentBoosts() {
        return recentBoosts;
    }

}
