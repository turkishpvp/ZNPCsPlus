package lol.pyr.znpcsplus.entity.properties;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.PacketEntity;
import lol.pyr.znpcsplus.util.NpcPath;
import org.bukkit.entity.Player;

import java.util.Map;

public class NpcPathProperty extends EntityPropertyImpl<NpcPath> {
    public NpcPathProperty() {
        super("path", NpcPath.EMPTY, NpcPath.class);
        setPlayerModifiable(false);
    }

    @Override
    public void apply(Player player, PacketEntity entity, boolean isSpawned, Map<Integer, EntityData<?>> properties) {
    }
}
