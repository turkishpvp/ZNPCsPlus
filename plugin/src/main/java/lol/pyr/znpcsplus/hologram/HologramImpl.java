package lol.pyr.znpcsplus.hologram;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lol.pyr.znpcsplus.api.hologram.Hologram;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.packets.PacketFactory;
import lol.pyr.znpcsplus.util.FutureUtil;
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.Viewable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class HologramImpl extends Viewable implements Hologram {
    private final ConfigManager configManager;
    private final PacketFactory packetFactory;
    private final LegacyComponentSerializer textSerializer;
    private final EntityPropertyRegistryImpl propertyRegistry;

    private double offset = 0.0;
    private long refreshDelay = -1;
    private long lastRefresh = System.currentTimeMillis();
    private NpcLocation location;
    private final List<HologramLine<?>> lines = new ArrayList<>();
    private static final String LEGACY_COLOR_PATTERN = "(?i)[&\u00a7][0-9A-FK-ORX]";
    private static final String BLANK_PREFIX = "%blank_";

    public HologramImpl(EntityPropertyRegistryImpl propertyRegistry, ConfigManager configManager, PacketFactory packetFactory, LegacyComponentSerializer textSerializer, NpcLocation location) {
        this.propertyRegistry = propertyRegistry;
        this.configManager = configManager;
        this.packetFactory = packetFactory;
        this.textSerializer = textSerializer;
        this.location = location;
    }

    public void addTextLineComponent(Component line) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line);
        lines.add(newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    private void addTextLineComponent(Component line, double lineSpacingMultiplier) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line, Collections.emptyList(), 0L, lineSpacingMultiplier);
        lines.add(newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    private void addTextLineComponent(Component line, List<Component> frames, long intervalMillis) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line, frames, intervalMillis, 1.0);
        lines.add(newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    public void addTextLine(String line) {
        if (isBlankLine(line)) {
            addTextLineComponent(HologramText.blank(), getBlankSpacingMultiplier(line));
            return;
        }
        AnimatedText animatedText = parseAnimatedText(line);
        if (animatedText != null) {
            addTextLineComponent(animatedText.frames.get(0), animatedText.frames, animatedText.intervalMillis);
            return;
        }
        Component component = line.contains("§") ? Component.text(line) : MiniMessage.miniMessage().deserialize(line);
        addTextLineComponent(textSerializer.deserialize(textSerializer.serialize(component)));
    }

    public void addItemLineStack(org.bukkit.inventory.ItemStack item) {
        addItemLinePEStack(SpigotConversionUtil.fromBukkitItemStack(item));
    }

    public void addItemLine(String serializedItem) {
        addItemLinePEStack(HologramItem.deserialize(serializedItem));
    }

    public void addItemLinePEStack(ItemStack item) {
        HologramItem newLine = new HologramItem(this, propertyRegistry, packetFactory, null, item);
        lines.add(newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    public void addLine(String line) {
        if (line.toLowerCase().startsWith("item:")) {
            addItemLine(line.substring(5));
        } else {
            addTextLine(line);
        }
    }

    public Component getLineTextComponent(int index) {
        return ((HologramText) lines.get(index)).getValue();
    }

    public String getLine(int index) {
        if (lines.get(index) instanceof HologramItem) {
            return ((HologramItem) lines.get(index)).serialize();
        } else {
            Component component = getLineTextComponent(index);
            if (HologramText.isBlank(component)) {
                double multiplier = lines.get(index).getLineSpacingMultiplier();
                return multiplier == 1.0 ? "%blank%" : "%blank_" + multiplier + "%";
            }
            return textSerializer.serialize(component);
        }
    }

    public void removeLine(int index) {
        HologramLine<?> line = lines.remove(index);
        for (Player viewer : getViewers()) line.hide(viewer);
        relocateLines();
    }

    public List<HologramLine<?>> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void clearLines() {
        UNSAFE_hideAll();
        lines.clear();
    }

    public void insertTextLineComponent(int index, Component line) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line);
        lines.add(index, newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    private void insertTextLineComponent(int index, Component line, double lineSpacingMultiplier) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line, Collections.emptyList(), 0L, lineSpacingMultiplier);
        lines.add(index, newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    private void insertTextLineComponent(int index, Component line, List<Component> frames, long intervalMillis) {
        HologramText newLine = new HologramText(this, propertyRegistry, packetFactory, null, line, frames, intervalMillis, 1.0);
        lines.add(index, newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    public void insertTextLine(int index, String line) {
        if (isBlankLine(line)) {
            insertTextLineComponent(index, HologramText.blank(), getBlankSpacingMultiplier(line));
            return;
        }
        AnimatedText animatedText = parseAnimatedText(line);
        if (animatedText != null) {
            insertTextLineComponent(index, animatedText.frames.get(0), animatedText.frames, animatedText.intervalMillis);
            return;
        }
        insertTextLineComponent(index, textSerializer.deserialize(textSerializer.serialize(MiniMessage.miniMessage().deserialize(line))));
    }

    public void insertItemLineStack(int index, org.bukkit.inventory.ItemStack item) {
        insertItemLinePEStack(index, SpigotConversionUtil.fromBukkitItemStack(item));
    }

    public void insertItemLinePEStack(int index, ItemStack item) {
        HologramItem newLine = new HologramItem(this, propertyRegistry, packetFactory, null, item);
        lines.add(index, newLine);
        relocateLines();
        for (Player viewer : getViewers()) newLine.show(viewer.getPlayer());
    }

    public void insertItemLine(int index, String item) {
        insertItemLinePEStack(index, HologramItem.deserialize(item));
    }

    public void insertLine(int index, String line) {
        if (line.toLowerCase().startsWith("item:")) {
            insertItemLine(index, line.substring(5));
        } else {
            insertTextLine(index, line);
        }
    }

    @Override
    public int lineCount() {
        return lines.size();
    }

    @Override
    protected CompletableFuture<Void> UNSAFE_show(Player player) {
        return FutureUtil.allOf(lines.stream()
                .map(line -> line.show(player))
                .collect(Collectors.toList()));
    }

    @Override
    protected void UNSAFE_hide(Player player) {
        for (HologramLine<?> line : lines) line.hide(player);
    }

    @Override
    public long getRefreshDelay() {
        return refreshDelay;
    }

    @Override
    public void setRefreshDelay(long refreshDelay) {
        this.refreshDelay = refreshDelay;
    }

    public boolean shouldRefresh() {
        return refreshDelay != -1 && (System.currentTimeMillis() - lastRefresh) > refreshDelay;
    }

    public void refresh() {
        lastRefresh = System.currentTimeMillis();
        for (HologramLine<?> line : lines) for (Player viewer : getViewers()) line.refreshMeta(viewer);
    }

    public void tickAnimations() {
        for (HologramLine<?> line : lines) {
            if (!line.tickAnimation()) continue;
            for (Player viewer : getViewers()) line.refreshMeta(viewer);
        }
    }

    public void setLocation(NpcLocation location) {
        this.location = location;
        relocateLines();
    }

    private void relocateLines() {
        final double lineSpacing = configManager.getConfig().lineSpacing();
        double height = location.getY() + (lines.size() - 1) * lineSpacing + getOffset();
        for (HologramLine<?> line : lines) {
            line.setLocation(location.withY(height));
            height -= lineSpacing * line.getLineSpacingMultiplier();
        }
    }

    public void setOffset(double offset) {
        this.offset = offset;
        relocateLines();
    }

    public double getOffset() {
        return offset;
    }

    private static boolean isBlankLine(String line) {
        if (line == null) return true;
        String normalized = line.trim();
        if (normalized.equalsIgnoreCase("%blank%")) return true;
        if (normalized.toLowerCase().startsWith(BLANK_PREFIX) && normalized.endsWith("%")) return true;
        normalized = normalized.replaceAll(LEGACY_COLOR_PATTERN, "");
        normalized = normalized.replaceAll("<[^>]+>", "");
        return normalized.trim().isEmpty();
    }

    private static double getBlankSpacingMultiplier(String line) {
        if (line == null) return 1.0;
        String normalized = line.trim().toLowerCase();
        if (!normalized.startsWith(BLANK_PREFIX) || !normalized.endsWith("%")) return 1.0;
        try {
            return Double.parseDouble(normalized.substring(BLANK_PREFIX.length(), normalized.length() - 1));
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    private AnimatedText parseAnimatedText(String line) {
        if (line == null || !line.toLowerCase().startsWith("anim:")) return null;
        String[] parts = line.split(":", 3);
        if (parts.length != 3) return null;
        long intervalTicks;
        try {
            intervalTicks = Math.max(1L, Long.parseLong(parts[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
        List<Component> frames = Arrays.stream(parts[2].split("\\|"))
                .map(String::trim)
                .filter(frame -> !frame.isEmpty())
                .map(frame -> textSerializer.deserialize(textSerializer.serialize(MiniMessage.miniMessage().deserialize(frame))))
                .collect(Collectors.toList());
        if (frames.isEmpty()) return null;
        return new AnimatedText(frames, intervalTicks * 50L);
    }

    private static final class AnimatedText {
        private final List<Component> frames;
        private final long intervalMillis;

        private AnimatedText(List<Component> frames, long intervalMillis) {
            this.frames = frames;
            this.intervalMillis = intervalMillis;
        }
    }
}
