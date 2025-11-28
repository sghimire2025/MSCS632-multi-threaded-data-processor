package dev.sghimire.model;

public class DataTask {
    private final int id;
    private final String payload;
    private final boolean poisonPill;

    public DataTask(int id, String payload) {
        this(id, payload, false);
    }

    private DataTask(int id, String payload, boolean poisonPill) {
        this.id = id;
        this.payload = payload;
        this.poisonPill = poisonPill;
    }

    public static DataTask poisonPill() {
        return new DataTask(-1, "", true);
    }

    public int getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }

    @Override
    public String toString() {
        return "DataTask{id=" + id + ", payload='" + payload + "'}";
    }
}
