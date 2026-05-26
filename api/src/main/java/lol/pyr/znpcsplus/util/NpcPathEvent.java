package lol.pyr.znpcsplus.util;

public class NpcPathEvent {
    public enum Type {
        SWING_MAIN,
        SWING_OFF,
        SNEAK_START,
        SNEAK_STOP,
        JUMP,
        EQUIPMENT
    }

    private final long timeMillis;
    private final Type type;
    private final String data;

    public NpcPathEvent(long timeMillis, Type type, String data) {
        this.timeMillis = Math.max(0, timeMillis);
        this.type = type;
        this.data = data == null ? "" : data;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
