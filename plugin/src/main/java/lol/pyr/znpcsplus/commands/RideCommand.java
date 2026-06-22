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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RideCommand implements CommandHandler {
    private final NpcRegistryImpl npcRegistry;
    private final EntityPropertyImpl<String> vehicleNpcProperty;

    public RideCommand(NpcRegistryImpl npcRegistry, EntityPropertyRegistryImpl propertyRegistry) {
        this.npcRegistry = npcRegistry;
        this.vehicleNpcProperty = propertyRegistry.getByName("vehicle_npc", String.class);
    }

    @Override
    public void run(CommandContext context) throws CommandExecutionException {
        context.setUsage(context.getLabel() + " ride <passenger npc> <vehicle npc|clear>");
        NpcEntryImpl passenger = context.parse(NpcEntryImpl.class);
        String vehicleId = context.popString().toLowerCase();

        if (vehicleId.equals("clear") || vehicleId.equals("none") || vehicleId.equals("remove")) {
            passenger.getNpc().UNSAFE_setProperty(vehicleNpcProperty, null);
            passenger.getNpc().setVehicleId(null);
            context.send(Component.text("NPC " + passenger.getId() + " is no longer riding another NPC.", NamedTextColor.GREEN));
            return;
        }

        NpcEntryImpl vehicle = npcRegistry.getById(vehicleId);
        if (vehicle == null || !vehicle.isAllowCommandModification()) {
            context.halt(Component.text("Vehicle NPC with ID " + vehicleId + " does not exist.", NamedTextColor.RED));
        }
        if (vehicle == passenger) {
            context.halt(Component.text("An NPC cannot ride itself.", NamedTextColor.RED));
        }
        if (wouldCreateCycle(passenger, vehicle)) {
            context.halt(Component.text("That ride chain would create a loop.", NamedTextColor.RED));
        }

        passenger.getNpc().UNSAFE_setProperty(vehicleNpcProperty, vehicle.getId());
        passenger.getNpc().setVehicleId(vehicle.getNpc().getEntity().getEntityId());
        context.send(Component.text("NPC " + passenger.getId() + " is now riding NPC " + vehicle.getId() + ".", NamedTextColor.GREEN));
    }

    private boolean wouldCreateCycle(NpcEntryImpl passenger, NpcEntryImpl vehicle) {
        NpcEntryImpl current = vehicle;
        while (current != null) {
            if (current == passenger) return true;
            String nextVehicle = current.getNpc().getProperty(vehicleNpcProperty);
            if (nextVehicle == null || nextVehicle.trim().isEmpty()) return false;
            current = npcRegistry.getById(nextVehicle);
        }
        return false;
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestCollection(npcRegistry.getModifiableIds());
        if (context.argSize() == 2) {
            List<String> suggestions = new ArrayList<>(npcRegistry.getModifiableIds());
            suggestions.add("clear");
            suggestions.add("none");
            suggestions.add("remove");
            return context.suggestCollection(suggestions);
        }
        return Collections.emptyList();
    }
}
