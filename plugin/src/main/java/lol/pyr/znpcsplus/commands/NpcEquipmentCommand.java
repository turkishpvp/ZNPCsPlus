package lol.pyr.znpcsplus.commands;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.command.CommandHandler;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.entity.EntityPropertyImpl;
import lol.pyr.znpcsplus.entity.EntityPropertyRegistryImpl;
import lol.pyr.znpcsplus.npc.NpcEntryImpl;
import lol.pyr.znpcsplus.npc.NpcImpl;
import lol.pyr.znpcsplus.npc.NpcRegistryImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class NpcEquipmentCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;
    private final EntityPropertyRegistryImpl propertyRegistry;
    private final Map<UUID, NpcEntryImpl> editSessions = new HashMap<>();

    public NpcEquipmentCommand(NpcRegistryImpl npcRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.npcRegistry = npcRegistry;
        this.propertyRegistry = propertyRegistry;
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " equip <id> [slot|cancel|clear]");
        Player player = context.ensureSenderIsPlayer();
        NpcEntryImpl entry = context.parse(NpcEntryImpl.class);

        if (context.argSize() == 0) {
            editSessions.put(player.getUniqueId(), entry);
            context.send(Component.text("Equipment edit mode enabled for " + entry.getId() + ". Right-click the NPC with an item to equip it. Sneak right-click equips offhand.", NamedTextColor.GREEN));
            return;
        }

        String slot = context.popString().toLowerCase();
        if (slot.equals("cancel")) {
            editSessions.remove(player.getUniqueId());
            context.send(Component.text("Equipment edit mode disabled.", NamedTextColor.YELLOW));
            return;
        }
        if (slot.equals("clear")) {
            clearEquipment(entry.getNpc());
            context.send(Component.text("Cleared equipment for " + entry.getId() + ".", NamedTextColor.GREEN));
            return;
        }
        if (!equip(entry.getNpc(), slot, player.getInventory().getItemInHand())) {
            context.halt(Component.text("Slot " + slot + " is not supported for this NPC type.", NamedTextColor.RED));
        }
        context.send(Component.text("Equipped " + slot + " for " + entry.getId() + ".", NamedTextColor.GREEN));
    }

    public boolean handleNpcClick(Player player, NpcEntryImpl clickedEntry, InteractionType interactionType) {
        if (interactionType != InteractionType.RIGHT_CLICK) return false;
        NpcEntryImpl target = editSessions.get(player.getUniqueId());
        if (target == null || !target.getId().equals(clickedEntry.getId())) return false;

        ItemStack itemStack = player.getInventory().getItemInHand();
        String slot = player.isSneaking() ? "offhand" : detectSlot(itemStack);
        if (!equip(clickedEntry.getNpc(), slot, itemStack)) return true;
        player.sendMessage("Equipped " + slot + " for NPC " + clickedEntry.getId() + ".");
        return true;
    }

    private boolean equip(NpcImpl npc, String slot, ItemStack itemStack) {
        EntityPropertyImpl<?> property = propertyRegistry.getByName(slot);
        if (property == null || !npc.getType().getAllowedProperties().contains(property)) {
            return false;
        }
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            npc.UNSAFE_setProperty(property, null);
            return true;
        }
        npc.UNSAFE_setProperty(property, SpigotConversionUtil.fromBukkitItemStack(itemStack));
        return true;
    }

    private void clearEquipment(NpcImpl npc) {
        for (String slot : Arrays.asList("helmet", "chestplate", "leggings", "boots", "hand", "offhand", "body", "saddle")) {
            EntityPropertyImpl<?> property = propertyRegistry.getByName(slot);
            if (property != null && npc.getType().getAllowedProperties().contains(property)) npc.UNSAFE_setProperty(property, null);
        }
    }

    private static String detectSlot(ItemStack itemStack) {
        if (itemStack == null) return "hand";
        String type = itemStack.getType().name();
        if (type.endsWith("_HELMET") || type.equals("PLAYER_HEAD") || type.equals("SKULL_ITEM")) return "helmet";
        if (type.endsWith("_CHESTPLATE") || type.equals("ELYTRA")) return "chestplate";
        if (type.endsWith("_LEGGINGS")) return "leggings";
        if (type.endsWith("_BOOTS")) return "boots";
        return "hand";
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) return context.suggestLiteral("hand", "offhand", "helmet", "chestplate", "leggings", "boots", "body", "saddle", "clear", "cancel");
        return Collections.emptyList();
    }
}
