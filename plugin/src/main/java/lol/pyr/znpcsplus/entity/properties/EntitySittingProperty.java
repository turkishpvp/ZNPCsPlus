package lol.pyr.znpcsplus.entity.properties;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lol.pyr.znpcsplus.entity.ArmorStandVehicleProperties;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import lol.pyr.znpcsplus.packets.PacketFactory;
import org.bukkit.entity.Player;

import java.util.Map;

public class EntitySittingProperty extends EntityPropertyImpl<Boolean> {
    private final PacketFactory packetFactory;
    private final EntityPropertyRegistryImpl propertyRegistry;

    public EntitySittingProperty(PacketFactory packetFactory, EntityPropertyRegistryImpl propertyRegistry) {
        this("entity_sitting", packetFactory, propertyRegistry);
    }

    public EntitySittingProperty(String name, PacketFactory packetFactory, EntityPropertyRegistryImpl propertyRegistry) {
        super(name, false, Boolean.class);
        this.packetFactory = packetFactory;
        this.propertyRegistry = propertyRegistry;
    }

    @Override
    public void apply(Player player, PacketEntity entity, boolean isSpawned, Map<Integer, EntityData<?>> properties) {
        boolean sitting = entity.getProperty(this);
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14)
                && entity.getType().isInstanceOf(EntityTypes.LIVINGENTITY)) {
            properties.put(6, newEntityData(6, EntityDataTypes.ENTITY_POSE, sitting ? EntityPose.SITTING : EntityPose.STANDING));
        }
        if (sitting) {
            double offset = getName().equals("player_sitting") ? getPlayerSittingHeight(entity) : -0.9;
            if (entity.getVehicle() == null) {
                PacketEntity vehiclePacketEntity = new PacketEntity(packetFactory, new ArmorStandVehicleProperties(propertyRegistry),
                        entity.getViewable(), EntityTypes.ARMOR_STAND, entity.getLocation().withY(entity.getLocation().getY() + offset));
                entity.setVehicle(vehiclePacketEntity, offset);
            } else {
                entity.updateVehicleOffset(offset);
            }
        } else if (entity.getVehicle() != null) {
            entity.setVehicle(null);
        }
    }

    @SuppressWarnings("unchecked")
    private double getPlayerSittingHeight(PacketEntity entity) {
        EntityPropertyImpl<Double> property = propertyRegistry.getByName("player_sitting_height", Double.class);
        if (property == null) return -1.35;
        return entity.getProperty(property);
    }
}
