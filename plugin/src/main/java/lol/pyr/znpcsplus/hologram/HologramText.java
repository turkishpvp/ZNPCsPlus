package lol.pyr.znpcsplus.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.packets.PacketFactory;
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.Viewable;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HologramText extends HologramLine<Component> {

    private static final Component BLANK  = Component.text("%blank%");
    private final List<Component> frames;
    private final long frameIntervalMillis;
    private int frameIndex;
    private long lastFrameChange = System.currentTimeMillis();

    public static Component blank() {
        return BLANK;
    }

    public static boolean isBlank(Component component) {
        return BLANK.equals(component);
    }

    public HologramText(Viewable viewable,  EntityPropertyRegistryImpl propertyRegistry, PacketFactory packetFactory, NpcLocation location, Component text) {
        this(viewable, propertyRegistry, packetFactory, location, text, Collections.emptyList(), 0L, 1.0);
    }

    public HologramText(Viewable viewable, EntityPropertyRegistryImpl propertyRegistry, PacketFactory packetFactory, NpcLocation location,
                        Component text, List<Component> frames, long frameIntervalMillis, double lineSpacingMultiplier) {
        super(viewable, text, packetFactory, EntityTypes.ARMOR_STAND, location);
        this.frames = frames;
        this.frameIntervalMillis = frameIntervalMillis;
        setLineSpacingMultiplier(lineSpacingMultiplier);
        addProperty(propertyRegistry.getByName("name"));
        addProperty(propertyRegistry.getByName("invisible"));
    }

    @Override
    public CompletableFuture<Void> show(Player player) {
        if (isBlank(getValue())) return CompletableFuture.completedFuture(null);
        return super.show(player);
    }

    @Override
    public boolean tickAnimation() {
        if (frames.isEmpty() || frameIntervalMillis <= 0) return false;
        long now = System.currentTimeMillis();
        if (now - lastFrameChange < frameIntervalMillis) return false;
        lastFrameChange = now;
        frameIndex = (frameIndex + 1) % frames.size();
        setValue(frames.get(frameIndex));
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(EntityProperty<T> key) {
        if (key.getName().equalsIgnoreCase("invisible")) return (T) Boolean.TRUE;
        if (key.getName().equalsIgnoreCase("name")) return (T) getValue();
        return super.getProperty(key);
    }

    @Override
    public boolean hasProperty(EntityProperty<?> key) {
        return key.getName().equalsIgnoreCase("name") || key.getName().equalsIgnoreCase("invisible");
    }
}
