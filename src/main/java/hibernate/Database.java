package hibernate;

public enum Database {

    BOT("Lawliet"), WEB("Web");


    private final String internalName;

    Database(String internalName) {
        this.internalName = internalName;
    }

    public String getInternalName() {
        return internalName;
    }
}
