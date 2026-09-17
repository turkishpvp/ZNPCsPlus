package lol.pyr.znpcsplus.interaction.queue;

import java.time.Duration;
import java.util.List;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.config.QueueConfig;
import lol.pyr.znpcsplus.interaction.InteractionActionImpl;
import lol.pyr.znpcsplus.util.PhoenixQueues;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import xyz.refinedev.queue.queue.impl.Queue;

/**
 * Puts the clicking player into a Phoenix queue. Clicking the npc of the queue they are already
 * waiting in takes them back out, unless that is disabled in the config. Feedback can be any
 * amount of chat lines plus a title and subtitle.
 */
public class QueueAction extends InteractionActionImpl {
    private final BukkitAudiences adventure;
    private final LegacyComponentSerializer textSerializer;
    private final ConfigManager configManager;
    private final String queueName;

    public QueueAction(BukkitAudiences adventure, LegacyComponentSerializer textSerializer, ConfigManager configManager,
                       String queueName, InteractionType interactionType, long cooldown, long delay) {
        super(cooldown, delay, interactionType);
        this.adventure = adventure;
        this.textSerializer = textSerializer;
        this.configManager = configManager;
        this.queueName = queueName;
    }

    @Override
    public void run(Player player) {
        QueueConfig config = configManager.getConfig().queueConfig();
        Queue target = PhoenixQueues.byName(queueName);
        if (!PhoenixQueues.available() || target == null) {
            send(player, config, config.unavailableChat(), config.unavailableTitle(), config.unavailableSubtitle(),
                queueName, 0, 0);
            return;
        }

        Queue current = PhoenixQueues.current(player.getUniqueId());
        boolean sameQueue = current != null && queueName.equalsIgnoreCase(current.getName());
        String display = PhoenixQueues.displayName(target, queueName);

        if (sameQueue) {
            if (!config.leaveOnSecondClick()) {
                send(player, config, config.alreadyQueuedChat(), config.alreadyQueuedTitle(), config.alreadyQueuedSubtitle(),
                    display, PhoenixQueues.position(player.getUniqueId()), PhoenixQueues.size(current));
                return;
            }
            PhoenixQueues.leave(player.getUniqueId());
            send(player, config, config.leftChat(), config.leftTitle(), config.leftSubtitle(),
                display, 0, PhoenixQueues.size(target));
            return;
        }

        boolean switched = current != null;
        if (!PhoenixQueues.join(player.getUniqueId(), queueName)) {
            send(player, config, config.unavailableChat(), config.unavailableTitle(), config.unavailableSubtitle(),
                display, 0, 0);
            return;
        }
        int position = PhoenixQueues.position(player.getUniqueId());
        int size = PhoenixQueues.size(target);
        if (switched) {
            send(player, config, config.switchedChat(), config.switchedTitle(), config.switchedSubtitle(), display, position, size);
        } else {
            send(player, config, config.joinedChat(), config.joinedTitle(), config.joinedSubtitle(), display, position, size);
        }
    }

    private void send(Player player, QueueConfig config, List<String> chat, String title, String subtitle,
                      String queue, int position, int size) {
        Audience audience = adventure.player(player);
        if (chat != null) {
            for (String line : chat) {
                if (line == null) {
                    continue;
                }
                audience.sendMessage(textSerializer.deserialize(replace(line, player, queue, position, size)));
            }
        }
        boolean hasTitle = title != null && !title.isEmpty();
        boolean hasSubtitle = subtitle != null && !subtitle.isEmpty();
        if (!hasTitle && !hasSubtitle) {
            return;
        }
        Title.Times times = Title.Times.times(
            ticks(config.titleFadeIn()),
            ticks(config.titleStay()),
            ticks(config.titleFadeOut()));
        audience.showTitle(Title.title(
            hasTitle ? textSerializer.deserialize(replace(title, player, queue, position, size)) : Component.empty(),
            hasSubtitle ? textSerializer.deserialize(replace(subtitle, player, queue, position, size)) : Component.empty(),
            times));
    }

    private Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private String replace(String text, Player player, String queue, int position, int size) {
        return text
            .replace("{queue}", queue == null ? "" : queue)
            .replace("{position}", String.valueOf(position))
            .replace("{size}", String.valueOf(size))
            .replace("{player}", player.getName());
    }

    @Override
    public Component getInfo(String id, int index, CommandContext context) {
        return Component.text(index + ") ", NamedTextColor.GOLD)
                .append(Component.text("[EDIT]", NamedTextColor.DARK_GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to edit this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + context.getLabel() + " action edit " + id + " " + index + " queue " + getInteractionType().name() + " " + getCooldown()/1000 + " " + getDelay() + " " + queueName))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("[DELETE]", NamedTextColor.RED)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click to delete this action", NamedTextColor.GRAY)))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                "/" + context.getLabel() + " action delete " + id + " " + index)))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Queue: ", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.text("Click Type: " + getInteractionType().name() + " Cooldown: " + getCooldown()/1000 + " Delay: " + getDelay(), NamedTextColor.GRAY))))
                .append(Component.text(queueName, NamedTextColor.WHITE)));
    }

    public String getQueueName() {
        return queueName;
    }
}
