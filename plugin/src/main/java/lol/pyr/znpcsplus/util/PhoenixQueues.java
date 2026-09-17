package lol.pyr.znpcsplus.util;

import java.util.UUID;
import xyz.refinedev.queue.QueueAPI;
import xyz.refinedev.queue.queue.impl.Queue;

/**
 * Thin wrapper around Phoenix's queue api (pxQueue). Every call is guarded, so the plugin keeps
 * working on servers where Phoenix is not installed - the queue action simply reports it as
 * unavailable instead of throwing.
 */
public final class PhoenixQueues {
    private PhoenixQueues() {
    }

    /** Whether the queue api is present and loaded. */
    public static boolean available() {
        return api() != null;
    }

    /** Queue the player is currently waiting in, or {@code null} when they are not queued. */
    public static Queue current(UUID playerId) {
        QueueAPI api = api();
        if (api == null) {
            return null;
        }
        try {
            return api.isInQueue(playerId) ? api.getPlayerQueue(playerId) : null;
        } catch (Throwable error) {
            return null;
        }
    }

    public static Queue byName(String name) {
        QueueAPI api = api();
        if (api == null || name == null) {
            return null;
        }
        try {
            Queue exact = api.getByName(name);
            return exact != null ? exact : loose(api, name);
        } catch (Throwable error) {
            return null;
        }
    }

    /**
     * Queue names come from the Phoenix panel, so the name written on an npc action may differ in
     * case or spacing from the one the panel stores. Match on the letters and digits only.
     */
    private static Queue loose(QueueAPI api, String name) {
        String wanted = simplify(name);
        if (wanted.isEmpty()) {
            return null;
        }
        for (String known : names()) {
            if (simplify(known).equals(wanted)) {
                try {
                    return api.getByName(known);
                } catch (Throwable error) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Names of the queues Phoenix knows about, empty when it is not installed. */
    public static java.util.List<String> names() {
        java.util.List<String> names = new java.util.ArrayList<>();
        QueueAPI api = api();
        if (api == null) {
            return names;
        }
        try {
            for (java.lang.reflect.Field field : api.getClass().getDeclaredFields()) {
                if (!java.util.Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(api);
                if (value instanceof java.util.Map) {
                    for (Object key : ((java.util.Map<?, ?>) value).keySet()) {
                        names.add(String.valueOf(key));
                    }
                }
            }
        } catch (Throwable ignored) {
            // Queue listing is a convenience, never fail an npc click over it.
        }
        return names;
    }

    private static String simplify(String value) {
        StringBuilder simple = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                simple.append(Character.toLowerCase(c));
            }
        }
        return simple.toString();
    }

    /** Puts the player into the queue, returns {@code false} when the queue does not exist. */
    public static boolean join(UUID playerId, String queueName) {
        QueueAPI api = api();
        Queue queue = byName(queueName);
        if (api == null || queue == null) {
            return false;
        }
        try {
            // Use the queue's own name: the npc action may spell it differently.
            api.addPlayerToQueue(playerId, queue.getName());
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    public static boolean leave(UUID playerId) {
        QueueAPI api = api();
        if (api == null) {
            return false;
        }
        try {
            api.removePlayerFromQueue(playerId);
            return true;
        } catch (Throwable error) {
            return false;
        }
    }

    /** Position of the player in their queue, {@code 0} when unknown. */
    public static int position(UUID playerId) {
        Queue queue = current(playerId);
        if (queue == null) {
            return 0;
        }
        try {
            return queue.getPosition(playerId);
        } catch (Throwable error) {
            return 0;
        }
    }

    public static int size(Queue queue) {
        if (queue == null) {
            return 0;
        }
        try {
            return queue.getAllQueued();
        } catch (Throwable error) {
            return 0;
        }
    }

    public static String displayName(Queue queue, String fallback) {
        if (queue == null) {
            return fallback;
        }
        try {
            String display = queue.getDisplayName();
            return display == null || display.isEmpty() ? queue.getName() : display;
        } catch (Throwable error) {
            return fallback;
        }
    }

    private static QueueAPI api() {
        try {
            return QueueAPI.INSTANCE;
        } catch (Throwable error) {
            // pxQueue / Phoenix is not on the server at all.
            return null;
        }
    }
}
