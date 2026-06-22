package lol.pyr.znpcsplus.entity.properties;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UsingItemProperty extends EntityPropertyImpl<Boolean> {
    private final int index;
    private final boolean entityFlag;

    public UsingItemProperty(int index, boolean entityFlag) {
        super("using_item", false, Boolean.class);
        this.index = index;
        this.entityFlag = entityFlag;
    }

    @Override
    public void apply(Player player, PacketEntity entity, boolean isSpawned, Map<Integer, EntityData<?>> properties) {
        boolean enabled = entity.getProperty(this);
        if (!entityFlag) {
            properties.put(index, newEntityData(index, EntityDataTypes.BYTE, (byte) (enabled ? 0x01 : 0)));
            return;
        }

        EntityData<?> oldData = properties.get(index);
        byte oldValue = 0;
        if (oldData != null && oldData.getValue() instanceof Number) {
            oldValue = ((Number) oldData.getValue()).byteValue();
        }
        properties.put(index, newEntityData(index, EntityDataTypes.BYTE, (byte) (oldValue | (enabled ? 0x10 : 0))));
    }

    public List<EntityData<?>> buildStandaloneData(boolean enabled, byte entityFlags) {
        if (entityFlag) {
            return Collections.singletonList(newEntityData(index, EntityDataTypes.BYTE, (byte) (entityFlags | (enabled ? 0x10 : 0))));
        }
        return Collections.singletonList(newEntityData(index, EntityDataTypes.BYTE, (byte) (enabled ? 0x01 : 0)));
    }

    public boolean isEntityFlag() {
        return entityFlag;
    }
}
