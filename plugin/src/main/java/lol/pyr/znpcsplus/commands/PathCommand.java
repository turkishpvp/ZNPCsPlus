package lol.pyr.znpcsplus.commands;

import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.NpcPath;
import lol.pyr.znpcsplus.util.NpcPathEvent;
import lol.pyr.znpcsplus.util.ItemSerializationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PathCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;
    private final EntityPropertyImpl<NpcPath> pathProperty;
    private final Map<UUID, Recording> recordings = new HashMap<>();

    public PathCommand(NpcRegistryImpl npcRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.npcRegistry = npcRegistry;
        this.pathProperty = propertyRegistry.getByName("path", NpcPath.class);
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " path <record|point|finish|cancel|clear|speed|info>");
        if (context.argSize() == 0) {
            sendHelp(context);
            return;
        }

        String action = context.popString().toLowerCase();
        switch (action) {
            case "record":
            case "start":
            case "create":
                startRecording(context);
                break;
            case "point":
            case "addpoint":
            case "add":
                addPoint(context);
                break;
            case "finish":
            case "exit":
            case "save":
                finishRecording(context);
                break;
            case "cancel":
                cancelRecording(context);
                break;
            case "clear":
            case "remove":
                clearPath(context);
                break;
            case "speed":
                setSpeed(context);
                break;
            case "info":
                showInfo(context);
                break;
            default:
                sendHelp(context);
        }
    }

    private void startRecording(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " path record <id> [speed] [loop]");
        Player player = context.ensureSenderIsPlayer();
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        double speed = context.argSize() >= 1 ? Math.max(0.01, context.parse(Double.class)) : 1.0;
        boolean loop = context.argSize() >= 1 ? context.parse(Boolean.class) : true;

        Recording recording = new Recording(entry, speed, loop);
        recording.add(new NpcLocation(player.getLocation()));
        recordings.put(player.getUniqueId(), recording);
        context.send(Component.text("Path recording started for NPC " + entry.getId() + ". Walk, use /npc path point if needed, then /npc path finish.", NamedTextColor.GREEN));
    }

    private void addPoint(CommandContext context) throws CommandExecutionException {
        Player player = context.ensureSenderIsPlayer();
        Recording recording = getRecording(context, player);
        recording.add(new NpcLocation(player.getLocation()));
        context.send(Component.text("Added path point #" + recording.points.size() + ".", NamedTextColor.GREEN));
    }

    private void finishRecording(CommandContext context) throws CommandExecutionException {
        Player player = context.ensureSenderIsPlayer();
        Recording recording = getRecording(context, player);
        if (recording.points.size() < 2) context.halt(Component.text("A path needs at least two points.", NamedTextColor.RED));
        recordings.remove(player.getUniqueId());

        NpcPath path = new NpcPath(recording.points, recording.events, recording.speed, recording.loop);
        NpcImpl npc = recording.entry.getNpc();
        if (!Objects.equals(npc.getWorld(), player.getWorld())) npc.setWorld(player.getWorld());
        npc.setLocation(path.getPoints().get(0));
        npc.UNSAFE_setProperty(pathProperty, path);
        context.send(Component.text("Saved " + path.getPoints().size() + " path points for NPC " + recording.entry.getId() + ".", NamedTextColor.GREEN));
    }

    private void cancelRecording(CommandContext context) throws CommandExecutionException {
        Player player = context.ensureSenderIsPlayer();
        if (recordings.remove(player.getUniqueId()) == null) {
            context.halt(Component.text("You are not recording a path.", NamedTextColor.RED));
        }
        context.send(Component.text("Path recording cancelled.", NamedTextColor.YELLOW));
    }

    private void clearPath(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " path clear <id>");
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        entry.getNpc().UNSAFE_setProperty(pathProperty, null);
        context.send(Component.text("Cleared path for NPC " + entry.getId() + ".", NamedTextColor.GREEN));
    }

    private void setSpeed(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " path speed <id> <speed>");
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        double speed = Math.max(0.01, context.parse(Double.class));
        NpcPath oldPath = entry.getNpc().getProperty(pathProperty);
        if (oldPath == null || oldPath.isEmpty()) context.halt(Component.text("This NPC does not have a path.", NamedTextColor.RED));
        entry.getNpc().UNSAFE_setProperty(pathProperty, new NpcPath(oldPath.getPoints(), speed, oldPath.isLoop()));
        context.send(Component.text("Path speed for NPC " + entry.getId() + " set to " + speed + " blocks/sec.", NamedTextColor.GREEN));
    }

    private void showInfo(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " path info <id>");
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        NpcPath path = entry.getNpc().getProperty(pathProperty);
        int points = path == null ? 0 : path.getPoints().size();
        double speed = path == null ? 0 : path.getSpeed();
        context.send(Component.text("Path info for " + entry.getId() + ": " + points + " points, " + speed + " blocks/sec.", NamedTextColor.GREEN));
    }

    public void recordMove(Player player, Location to) {
        Recording recording = recordings.get(player.getUniqueId());
        if (recording == null) return;
        NpcImpl npc = recording.entry.getNpc();
        if (npc.getWorld() != null && !Objects.equals(npc.getWorld(), to.getWorld())) return;
        NpcLocation next = new NpcLocation(to);
        if (recording.isFarEnough(next)) recording.add(next);
        if (to.getY() - recording.lastY > 0.35) recording.addEvent(NpcPathEvent.Type.JUMP, "");
        recording.lastY = to.getY();
    }

    public void recordSwing(Player player, boolean offHand) {
        Recording recording = recordings.get(player.getUniqueId());
        if (recording == null) return;
        recording.addEvent(offHand ? NpcPathEvent.Type.SWING_OFF : NpcPathEvent.Type.SWING_MAIN, "");
        recordEquipment(player, offHand ? "offhand" : "hand", player.getInventory().getItemInHand());
    }

    public void recordSneak(Player player, boolean sneaking) {
        Recording recording = recordings.get(player.getUniqueId());
        if (recording == null) return;
        recording.addEvent(sneaking ? NpcPathEvent.Type.SNEAK_START : NpcPathEvent.Type.SNEAK_STOP, "");
    }

    public void recordEquipment(Player player, String slot, ItemStack itemStack) {
        Recording recording = recordings.get(player.getUniqueId());
        if (recording == null || itemStack == null || itemStack.getType() == Material.AIR) return;
        recording.addEvent(NpcPathEvent.Type.EQUIPMENT, slot + ":" + ItemSerializationUtil.itemToB64(itemStack));
    }

    private Recording getRecording(CommandContext context, Player player) throws CommandExecutionException {
        Recording recording = recordings.get(player.getUniqueId());
        if (recording == null) context.halt(Component.text("You are not recording a path.", NamedTextColor.RED));
        return recording;
    }

    private void sendHelp(CommandContext context) {
        context.send(Component.text("Path: /npc path record <id> [speed] [loop], /npc path finish, /npc path clear <id>, /npc path speed <id> <speed>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestLiteral("record", "point", "finish", "cancel", "clear", "speed", "info");
        String action = context.suggestionParse(0, String.class).toLowerCase();
        if (context.argSize() == 2 && Arrays.asList("record", "clear", "speed", "info").contains(action)) {
            return context.suggestCollection(npcRegistry.getModifiableIds());
        }
        if (context.argSize() == 3 && action.equals("speed")) return context.suggestLiteral("1.0", "2.0", "4.0");
        if (context.argSize() == 3 && action.equals("record")) return context.suggestLiteral("1.0", "2.0", "4.0");
        if (context.argSize() == 4 && action.equals("record")) return context.suggestLiteral("true", "false");
        return Collections.emptyList();
    }

    private static final class Recording {
        private final NpcEntryImpl entry;
        private final List<NpcLocation> points = new ArrayList<>();
        private final List<NpcPathEvent> events = new ArrayList<>();
        private final double speed;
        private final boolean loop;
        private final long started = System.currentTimeMillis();
        private double lastY;

        private Recording(NpcEntryImpl entry, double speed, boolean loop) {
            this.entry = entry;
            this.speed = speed;
            this.loop = loop;
            this.lastY = entry.getNpc().getLocation().getY();
        }

        private void add(NpcLocation location) {
            points.add(location);
            lastY = location.getY();
        }

        private void addEvent(NpcPathEvent.Type type, String data) {
            long elapsed = System.currentTimeMillis() - started;
            if (!events.isEmpty()) {
                NpcPathEvent last = events.get(events.size() - 1);
                if (last.getType() == type && last.getData().equals(data) && elapsed - last.getTimeMillis() < 150) return;
            }
            events.add(new NpcPathEvent(elapsed, type, data));
        }

        private boolean isFarEnough(NpcLocation location) {
            if (points.isEmpty()) return true;
            NpcLocation last = points.get(points.size() - 1);
            double x = location.getX() - last.getX();
            double y = location.getY() - last.getY();
            double z = location.getZ() - last.getZ();
            return (x * x) + (y * y) + (z * z) >= 1.0;
        }
    }
}
