package lol.pyr.znpcsplus.entity.serializers;

import lol.pyr.znpcsplus.entity.PropertySerializer;
import lol.pyr.znpcsplus.util.LookType;

public class LookTypeSerializer implements PropertySerializer<LookType> {
    @Override
    public String serialize(LookType property) {
        return property.name();
    }

    @Override
    public LookType deserialize(String property) {
        String normalized = property.toLowerCase().replace('-', '_');
        if (normalized.equals("true") || normalized.equals("close") || normalized.equals("closest") || normalized.equals("closest_player")) return LookType.CLOSEST_PLAYER;
        if (normalized.equals("perplayer") || normalized.equals("per_player")) return LookType.PER_PLAYER;
        if (normalized.equals("false") || normalized.equals("fixed")) return LookType.FIXED;
        try {
             return LookType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return LookType.FIXED;
        }
    }

    @Override
    public Class<LookType> getTypeClass() {
        return LookType.class;
    }
}
