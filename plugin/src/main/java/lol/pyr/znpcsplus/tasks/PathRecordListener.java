package lol.pyr.znpcsplus.tasks;

import lol.pyr.znpcsplus.commands.PathCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class PathRecordListener implements Listener {
    private final PathCommand pathCommand;

    public PathRecordListener(PathCommand pathCommand) {
        this.pathCommand = pathCommand;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        pathCommand.recordMove(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            pathCommand.recordSwing(event.getPlayer(), false);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            pathCommand.recordSwing(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        pathCommand.recordSneak(event.getPlayer(), event.isSneaking());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        pathCommand.recordEquipment(event.getPlayer(), "hand", event.getPlayer().getInventory().getItem(event.getNewSlot()));
    }
}
