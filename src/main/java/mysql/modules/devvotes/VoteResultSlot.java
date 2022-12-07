package mysql.modules.devvotes;

public class VoteResultSlot {

    private final String id;
    private final int number;

    public VoteResultSlot(String id, int number) {
        this.id = id;
        this.number = number;
    }

    public String getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

}
