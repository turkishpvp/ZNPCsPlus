package lol.pyr.znpcsplus.npc;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lol.pyr.znpcsplus.api.entity.EntityProperty;
import lol.pyr.znpcsplus.api.interaction.InteractionAction;
import lol.pyr.znpcsplus.api.npc.Npc;
import lol.pyr.znpcsplus.api.npc.NpcType;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import lol.pyr.znpcsplus.hologram.HologramImpl;
import lol.pyr.znpcsplus.packets.PacketFactory;
import lol.pyr.znpcsplus.util.NamedColor;
import lol.pyr.znpcsplus.util.NpcPath;
import lol.pyr.znpcsplus.util.NpcPathEvent;
import lol.pyr.znpcsplus.util.NpcLocation;
import lol.pyr.znpcsplus.util.NpcPose;
import lol.pyr.znpcsplus.util.Viewable;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NpcImpl extends Viewable implements Npc {
    private final PacketFactory packetFactory;
    private String worldName;
    private PacketEntity entity;
    private NpcLocation location;
    private NpcTypeImpl type;
    private boolean enabled = true;
    private final HologramImpl hologram;
    private final UUID uuid;

    private final Map<EntityPropertyImpl<?>, Object> propertyMap = new HashMap<>();
    private final List<InteractionAction> actions = new ArrayList<>();

    private final Map<UUID, float[]> playerLookMap = new ConcurrentHashMap<>();
    private NpcPath activePath;
    private int pathTargetIndex = 1;
    private long lastPathMove = System.currentTimeMillis();
    private long pathStarted = System.currentTimeMillis();
    private int pathEventIndex = 0;
    private boolean shiftAnimationState;
    private long lastShiftAnimation = System.currentTimeMillis();
    private long lastSwingAnimation = System.currentTimeMillis();

    private final EntityPropertyImpl<Double> attributeScaleProperty;
    private final EntityPropertyImpl<NpcPose> poseProperty;

    protected NpcImpl(UUID uuid, EntityPropertyRegistryImpl propertyRegistry, ConfigManager configManager, LegacyComponentSerializer textSerializer, World world, NpcTypeImpl type, NpcLocation location, PacketFactory packetFactory) {
        this(uuid, propertyRegistry, configManager, packetFactory, textSerializer, world.getName(), type, location);
    }

    public NpcImpl(UUID uuid, EntityPropertyRegistryImpl propertyRegistry, ConfigManager configManager, PacketFactory packetFactory, LegacyComponentSerializer textSerializer, String world, NpcTypeImpl type, NpcLocation location) {
        this.packetFactory = packetFactory;
        this.worldName = world;
        this.type = type;
        this.location = location;
        this.uuid = uuid;
        entity = new PacketEntity(packetFactory, this, this, type.getType(), location);
        hologram = new HologramImpl(propertyRegistry, configManager, packetFactory, textSerializer, location.withY(location.getY() + type.getHologramOffset()));
        this.attributeScaleProperty = propertyRegistry.getByName("attribute_scale", Double.class);
        this.poseProperty = propertyRegistry.getByName("pose", NpcPose.class);
    }

    public void setType(NpcTypeImpl type) {
        UNSAFE_hideAll();
        this.type = type;
        entity = new PacketEntity(packetFactory, this, this, type.getType(), entity.getLocation());
        hologram.setLocation(location.withY(location.getY() + type.getHologramOffset()));
        UNSAFE_showAll();
    }

    public void setType(NpcType type) {
        if (type == null) throw new IllegalArgumentException("Npc Type cannot be null");
        setType((NpcTypeImpl) type);
    }

    public NpcTypeImpl getType() {
        return type;
    }

    public PacketEntity getEntity() {
        return entity;
    }

    public NpcLocation getLocation() {
        return location;
    }

    public NpcLocation getLocation(Player player) {
        if (playerLookMap.containsKey(player.getUniqueId())) {
            float[] rotation = playerLookMap.get(player.getUniqueId());
            return location.withRotation(rotation[0], rotation[1]);
        }
        return location;
    }

    public @Nullable Location getBukkitLocation() {
        World world = getWorld();
        if (world == null) return null;
        return location.toBukkitLocation(world);
    }

    public void setLocation(NpcLocation location) {
        this.location = location;
        playerLookMap.clear();
        playerLookMap.putAll(getViewers().stream().collect(Collectors.toMap(Player::getUniqueId, player -> new float[]{location.getYaw(), location.getPitch()})));
        NpcLocation finalLocation = location;
        if (type.getType().isInstanceOf(EntityTypes.ENDER_DRAGON)) {
            finalLocation = finalLocation.withRotation(location.getYaw() + 180, location.getPitch());
        }
        entity.setLocation(finalLocation);
        hologram.setLocation(finalLocation.withY(finalLocation.getY() + type.getHologramOffset()));
    }

    public void processPath(EntityPropertyImpl<NpcPath> pathProperty) {
        if (pathProperty == null) return;
        NpcPath path = getProperty(pathProperty);
        if (path == null || path.isEmpty()) {
            activePath = null;
            return;
        }
        if (activePath != path) {
            activePath = path;
            pathTargetIndex = findNearestTargetIndex(path);
            lastPathMove = System.currentTimeMillis();
            pathStarted = lastPathMove;
            pathEventIndex = 0;
        }

        long now = System.currentTimeMillis();
        processPathEvents(path, now);
        double movementLeft = path.getSpeed() * ((now - lastPathMove) / 1000.0);
        lastPathMove = now;
        while (movementLeft > 0 && !path.isEmpty()) {
            NpcLocation target = path.getPoints().get(pathTargetIndex);
            double x = target.getX() - location.getX();
            double y = target.getY() - location.getY();
            double z = target.getZ() - location.getZ();
            double distance = Math.sqrt((x * x) + (y * y) + (z * z));

            if (distance <= movementLeft || distance < 0.01) {
                setLocation(target);
                movementLeft -= distance;
                if (!advancePathTarget(path)) return;
                continue;
            }

            double factor = movementLeft / distance;
            float yaw = target.getYaw();
            float pitch = target.getPitch();
            if (x != 0 || z != 0) {
                NpcLocation look = location.lookingAt(target);
                yaw = look.getYaw();
                pitch = look.getPitch();
            }
            setLocation(new NpcLocation(
                    location.getX() + (x * factor),
                    location.getY() + (y * factor),
                    location.getZ() + (z * factor),
                    yaw,
                    pitch
            ));
            movementLeft = 0;
        }
    }

    public void processPlayerAnimations(EntityPropertyImpl<Boolean> shiftAnimationProperty, EntityPropertyImpl<Boolean> swingAnimationProperty,
                                        EntityPropertyImpl<Boolean> fireProperty, EntityPropertyImpl<Boolean> invisibleProperty,
                                        EntityPropertyImpl<NamedColor> glowProperty) {
        if (!type.getType().equals(EntityTypes.PLAYER)) return;
        long now = System.currentTimeMillis();
        if (shiftAnimationProperty != null && getProperty(shiftAnimationProperty) && now - lastShiftAnimation >= 500L) {
            lastShiftAnimation = now;
            shiftAnimationState = !shiftAnimationState;
            byte flags = 0;
            if (fireProperty != null && getProperty(fireProperty)) flags |= 0x01;
            if (shiftAnimationState) flags |= 0x02;
            if (invisibleProperty != null && getProperty(invisibleProperty)) flags |= 0x20;
            if (glowProperty != null && getProperty(glowProperty) != null) flags |= 0x40;
            List<EntityData<?>> data = Collections.singletonList(new EntityData<>(0, EntityDataTypes.BYTE, flags));
            for (Player viewer : getViewers()) packetFactory.sendMetadata(viewer, entity, data);
        }
        if (swingAnimationProperty != null && getProperty(swingAnimationProperty) && now - lastSwingAnimation >= 600L) {
            lastSwingAnimation = now;
            swingHand(false);
        }
    }

    private int findNearestTargetIndex(NpcPath path) {
        int nearestIndex = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < path.getPoints().size(); i++) {
            NpcLocation point = path.getPoints().get(i);
            double x = point.getX() - location.getX();
            double y = point.getY() - location.getY();
            double z = point.getZ() - location.getZ();
            double distance = (x * x) + (y * y) + (z * z);
            if (distance >= nearestDistance) continue;
            nearestDistance = distance;
            nearestIndex = i;
        }
        int target = nearestIndex + 1;
        return target >= path.getPoints().size() ? 0 : target;
    }

    private boolean advancePathTarget(NpcPath path) {
        pathTargetIndex++;
        if (pathTargetIndex < path.getPoints().size()) return true;
        if (!path.isLoop()) {
            pathTargetIndex = path.getPoints().size() - 1;
            return false;
        }
        pathTargetIndex = 0;
        pathStarted = System.currentTimeMillis();
        pathEventIndex = 0;
        return true;
    }

    private void processPathEvents(NpcPath path, long now) {
        long elapsed = now - pathStarted;
        while (pathEventIndex < path.getEvents().size()) {
            NpcPathEvent event = path.getEvents().get(pathEventIndex);
            if (event.getTimeMillis() > elapsed) break;
            applyPathEvent(event);
            pathEventIndex++;
        }
    }

    private void applyPathEvent(NpcPathEvent event) {
        switch (event.getType()) {
            case SWING_MAIN:
                swingHand(false);
                break;
            case SWING_OFF:
                swingHand(true);
                break;
            case SNEAK_START:
                if (poseProperty != null) setProperty(poseProperty, NpcPose.CROUCHING);
                break;
            case SNEAK_STOP:
                if (poseProperty != null) setProperty(poseProperty, NpcPose.STANDING);
                break;
            case JUMP:
                setLocation(location.withY(location.getY() + 0.35));
                setLocation(location.withY(location.getY() - 0.35));
                break;
            case EQUIPMENT:
                applyRecordedEquipment(event.getData());
                break;
        }
    }

    private void applyRecordedEquipment(String data) {
        String[] split = data.split(":", 2);
        if (split.length != 2) return;
        EntityPropertyImpl<?> property = null;
        for (EntityProperty<?> candidate : type.getAllowedProperties()) {
            if (candidate.getName().equalsIgnoreCase(split[0])) {
                property = (EntityPropertyImpl<?>) candidate;
                break;
            }
        }
        if (property == null) return;
        if (!(property.getDefaultValue() instanceof com.github.retrooper.packetevents.protocol.item.ItemStack) && property.getDefaultValue() != null) return;
        try {
            com.github.retrooper.packetevents.protocol.item.ItemStack stack =
                    io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(lol.pyr.znpcsplus.util.ItemSerializationUtil.itemFromB64(split[1]));
            UNSAFE_setProperty(property, stack);
        } catch (RuntimeException ignored) {
        }
    }

    public void setHeadRotation(Player player, float yaw, float pitch) {
        if (getHeadYaw(player) == yaw && getHeadPitch(player) == pitch) return;
        playerLookMap.put(player.getUniqueId(), new float[]{yaw, pitch});
        if (type.getType().isInstanceOf(EntityTypes.ENDER_DRAGON)) {
            yaw += 180;
            if (yaw > 360) yaw -= 360;
        }
        entity.setHeadRotation(player, yaw, pitch);
    }

    public void setHeadRotation(float yaw, float pitch) {
        for (Player player : getViewers()) {
            if (getHeadYaw(player) == yaw && getHeadPitch(player) == pitch) continue;
            playerLookMap.put(player.getUniqueId(), new float[]{yaw, pitch});
            float playerYaw = yaw;
            if (type.getType().isInstanceOf(EntityTypes.ENDER_DRAGON)) {
                playerYaw += 180;
                if (playerYaw > 360) playerYaw -= 360;
            }
            entity.setHeadRotation(player, playerYaw, pitch);
        }
    }

    public NpcLocation lookingAt(Location target, float yawOffset, float pitchOffset) {
        double scale = attributeScaleProperty != null && hasProperty(attributeScaleProperty) ? getProperty(attributeScaleProperty) : 1.0f;
        float eyeHeight = type.getEyeHeight();

        return location.lookingAt(target, scale, eyeHeight, yawOffset, pitchOffset);
    }

    public NpcLocation lookingAt(Player target, float yawOffset, float pitchOffset) {
        return lookingAt(target.getEyeLocation(), yawOffset, pitchOffset);
    }

    public float getHeadYaw(Player player) {
        return playerLookMap.getOrDefault(player.getUniqueId(), new float[]{location.getYaw(), location.getPitch()})[0];
    }

    public float getHeadPitch(Player player) {
        return playerLookMap.getOrDefault(player.getUniqueId(), new float[]{location.getYaw(), location.getPitch()})[1];
    }

    public HologramImpl getHologram() {
        return hologram;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) delete();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Nullable World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public String getWorldName() {
        return worldName;
    }

    @Override
    protected CompletableFuture<Void> UNSAFE_show(Player player) {
        playerLookMap.put(player.getUniqueId(), new float[]{location.getYaw(), location.getPitch()});
        return CompletableFuture.allOf(entity.spawn(player), hologram.show(player));
    }

    @Override
    protected void UNSAFE_hide(Player player) {
        playerLookMap.remove(player.getUniqueId());
        entity.despawn(player);
        hologram.hide(player);
    }

    private <T> void UNSAFE_refreshProperty(EntityPropertyImpl<T> property) {
        if (!type.isAllowedProperty(property)) return;
        for (Player viewer : getViewers()) {
            List<EntityData<?>> data = property.applyStandalone(viewer, entity, true);
            if (!data.isEmpty()) packetFactory.sendMetadata(viewer, entity, data);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(EntityProperty<T> key) {
        return hasProperty(key) ? (T) propertyMap.get((EntityPropertyImpl<?>) key) : key.getDefaultValue();
    }

    public boolean hasProperty(EntityProperty<?> key) {
        return propertyMap.containsKey((EntityPropertyImpl<?>) key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void setProperty(EntityProperty<T> key, T value) {
        // See https://github.com/Pyrbu/ZNPCsPlus/pull/129#issuecomment-1948777764
        Object val = value;
        if (val instanceof ItemStack) val = SpigotConversionUtil.fromBukkitItemStack((ItemStack) val);

        setProperty((EntityPropertyImpl<T>) key, (T) val);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setItemProperty(EntityProperty<?> key, ItemStack value) {
        setProperty((EntityPropertyImpl<com.github.retrooper.packetevents.protocol.item.ItemStack>) key, SpigotConversionUtil.fromBukkitItemStack(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ItemStack getItemProperty(EntityProperty<?> key) {
        return SpigotConversionUtil.toBukkitItemStack(getProperty((EntityProperty<com.github.retrooper.packetevents.protocol.item.ItemStack>) key));
    }

    public <T> void setProperty(EntityPropertyImpl<T> key, T value) {
        if (key == null) return;
        if (value == null || value.equals(key.getDefaultValue())) propertyMap.remove(key);
        else propertyMap.put(key, value);
        UNSAFE_refreshProperty(key);
    }

    @SuppressWarnings("unchecked")
    public <T> void UNSAFE_setProperty(EntityPropertyImpl<?> property, Object value) {
        setProperty((EntityPropertyImpl<T>) property, (T) value);
    }

    @SuppressWarnings("unchecked")
    public <T> void UNSAFE_setProperty(EntityProperty<?> property, Object value) {
        setProperty((EntityPropertyImpl<T>) property, (T) value);
    }

    public Set<EntityProperty<?>> getAllProperties() {
        return Collections.unmodifiableSet(propertyMap.keySet());
    }

    @Override
    public Set<EntityProperty<?>> getAppliedProperties() {
        return Collections.unmodifiableSet(propertyMap.keySet()).stream().filter(type::isAllowedProperty).collect(Collectors.toSet());
    }

    @Override
    public List<InteractionAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    @Override
    public void removeAction(int index) {
        actions.remove(index);
    }

    @Override
    public void addAction(InteractionAction action) throws IllegalArgumentException {
        if (action == null) throw new IllegalArgumentException("action can not be null");
        actions.add(action);
    }

    @Override
    public void clearActions() {
        actions.clear();
    }

    @Override
    public void editAction(int index, InteractionAction action) throws IllegalArgumentException {
        if (action == null) throw new IllegalArgumentException("action can not be null");
        actions.set(index, action);
    }

    @Override
    public int getPacketEntityId() {
        return entity.getEntityId();
    }

    public void setWorld(World world) {
        if (world == null) throw new IllegalArgumentException("world can not be null");
        delete();
        this.worldName = world.getName();
    }

    public void setWorld(String name) {
        if (name == null) throw new IllegalArgumentException("world name can not be null");
        delete();
        this.worldName = name;
    }

    public void swingHand(boolean offHand) {
        for (Player viewer : getViewers()) entity.swingHand(viewer, offHand);
    }

    @Override
    public @NotNull List<Integer> getPassengers() {
        return entity.getPassengers();
    }

    @Override
    public void addPassenger(int entityId) {
        entity.addPassenger(entityId);
    }

    @Override
    public void removePassenger(int entityId) {
        entity.removePassenger(entityId);
    }

    @Override
    public @Nullable Integer getVehicleId() {
        return entity.getVehicleId();
    }

    @Override
    public void setVehicleId(Integer vehicleId) {
        entity.setVehicleId(vehicleId);
    }
}
