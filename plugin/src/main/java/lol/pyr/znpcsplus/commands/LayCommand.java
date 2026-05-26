package lol.pyr.znpcsplus.commands;

import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import lol.pyr.znpcsplus.util.NpcPose;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

public class LayCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;
    private final EntityPropertyImpl<NpcPose> poseProperty;

    public LayCommand(NpcRegistryImpl npcRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.npcRegistry = npcRegistry;
        this.poseProperty = propertyRegistry.getByName("pose", NpcPose.class);
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " lay <id> [true|false]");
        if (poseProperty == null) context.halt(Component.text("Laying requires Minecraft 1.14 or newer.", NamedTextColor.RED));
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        boolean value = context.argSize() >= 1 ? context.parse(Boolean.class) : entry.getNpc().getProperty(poseProperty) != NpcPose.SLEEPING;
        entry.getNpc().UNSAFE_setProperty(poseProperty, value ? NpcPose.SLEEPING : NpcPose.STANDING);
        context.send(Component.text("NPC " + entry.getId() + " laying: " + value, NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestLiteral("true", "false");
        return Collections.emptyList();
    }
}
