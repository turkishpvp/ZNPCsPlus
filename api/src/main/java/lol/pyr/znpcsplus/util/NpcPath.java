package lol.pyr.znpcsplus.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class NpcPath {
    public static final NpcPath EMPTY = new NpcPath(Collections.emptyList(), 1.0, true);

    private final List<NpcLocation> points;
    private final List<NpcPathEvent> events;
    private final double speed;
    private final boolean loop;

    public NpcPath(List<NpcLocation> points, double speed, boolean loop) {
        this(points, Collections.emptyList(), speed, loop);
    }

    public NpcPath(List<NpcLocation> points, List<NpcPathEvent> events, double speed, boolean loop) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        List<NpcPathEvent> sortedEvents = new ArrayList<>(events);
        sortedEvents.sort(Comparator.comparingLong(NpcPathEvent::getTimeMillis));
        this.events = Collections.unmodifiableList(sortedEvents);
        this.speed = Math.max(0.01, speed);
        this.loop = loop;
    }

    public List<NpcLocation> getPoints() {
        return points;
    }

    public double getSpeed() {
        return speed;
    }

    public List<NpcPathEvent> getEvents() {
        return events;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isEmpty() {
        return points.size() < 2;
    }

    public String serialize() {
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(Double.toString(speed));
        joiner.add(Boolean.toString(loop));
        for (NpcLocation point : points) {
            joiner.add(String.format(Locale.ROOT, "P,%f,%f,%f,%f,%f",
                    point.getX(), point.getY(), point.getZ(), point.getYaw(), point.getPitch()));
        }
        for (NpcPathEvent event : events) {
            joiner.add("E," + event.getTimeMillis() + "," + event.getType().name() + "," + event.getData());
        }
        return joiner.toString();
    }

    public static NpcPath deserialize(String input) {
        if (input == null || input.trim().isEmpty()) return EMPTY;
        String[] parts = input.split("\\|");
        if (parts.length < 4) return EMPTY;
        double speed = parseDouble(parts[0], 1.0);
        boolean loop = Boolean.parseBoolean(parts[1]);
        List<NpcLocation> points = new ArrayList<>();
        List<NpcPathEvent> events = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            String[] location = parts[i].split(",", 4);
            if (location.length >= 3 && location[0].equals("E")) {
                try {
                    events.add(new NpcPathEvent(Long.parseLong(location[1]), NpcPathEvent.Type.valueOf(location[2]), location.length == 4 ? location[3] : ""));
                } catch (IllegalArgumentException ignored) {
                }
                continue;
            }
            location = parts[i].startsWith("P,") ? parts[i].substring(2).split(",") : parts[i].split(",");
            if (location.length != 5) continue;
            points.add(new NpcLocation(
                    parseDouble(location[0], 0),
                    parseDouble(location[1], 0),
                    parseDouble(location[2], 0),
                    (float) parseDouble(location[3], 0),
                    (float) parseDouble(location[4], 0)
            ));
        }
        return points.size() < 2 ? EMPTY : new NpcPath(points, events, speed, loop);
    }

    private static double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
