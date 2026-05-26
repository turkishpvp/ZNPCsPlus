package lol.pyr.znpcsplus.commands;

import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

public class SitCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;
    private final EntityPropertyImpl<Boolean> sittingProperty;

    public SitCommand(NpcRegistryImpl npcRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.npcRegistry = npcRegistry;
        this.sittingProperty = propertyRegistry.getByName("entity_sitting", Boolean.class);
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " sit <id> [true|false]");
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);
        boolean value = context.argSize() >= 1 ? context.parse(Boolean.class) : !entry.getNpc().getProperty(sittingProperty);
        entry.getNpc().UNSAFE_setProperty(sittingProperty, value);
        context.send(Component.text("NPC " + entry.getId() + " sitting: " + value, NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestLiteral("true", "false");
        return Collections.emptyList();
    }
}
