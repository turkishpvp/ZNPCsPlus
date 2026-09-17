package lol.pyr.znpcsplus.interaction.queue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.znpcsplus.api.interaction.InteractionActionType;
import lol.pyr.znpcsplus.api.interaction.InteractionType;
import lol.pyr.znpcsplus.config.ConfigManager;
import lol.pyr.znpcsplus.interaction.InteractionActionImpl;
import lol.pyr.znpcsplus.interaction.InteractionCommandHandler;
import lol.pyr.znpcsplus.util.PhoenixQueues;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class QueueActionType implements InteractionActionType<QueueAction>, InteractionCommandHandler {
    private final BukkitAudiences adventure;
    private final LegacyComponentSerializer textSerializer;
    private final ConfigManager configManager;

    public QueueActionType(BukkitAudiences adventure, LegacyComponentSerializer textSerializer, ConfigManager configManager) {
        this.adventure = adventure;
        this.textSerializer = textSerializer;
        this.configManager = configManager;
    }

    @Override
    public String serialize(QueueAction obj) {
        return Base64.getEncoder().encodeToString(obj.getQueueName().getBytes(StandardCharsets.UTF_8))
                + ";" + obj.getCooldown() + ";" + obj.getInteractionType().name() + ";" + obj.getDelay();
    }

    @Override
    public QueueAction deserialize(String str) {
        String[] split = str.split(";");
        InteractionType type = split.length > 2 ? InteractionType.valueOf(split[2]) : InteractionType.ANY_CLICK;
        String queue = new String(Base64.getDecoder().decode(split[0]), StandardCharsets.UTF_8);
        return new QueueAction(adventure, textSerializer, configManager, queue, type,
                Long.parseLong(split[1]), Long.parseLong(split.length > 3 ? split[3] : "0"));
    }

    @Override
    public Class<QueueAction> getActionClass() {
        return QueueAction.class;
    }

    @Override
    public String getSubcommandName() {
        return "queue";
    }

    @Override
    public void appendUsage(CommandContext context) {
        context.setUsage(context.getUsage() + " " + getSubcommandName() + " <id> <click type> <cooldown seconds> <delay ticks> <queue name>");
    }

    @Override
    public InteractionActionImpl parse(CommandContext context) throws CommandExecutionException {
        InteractionType type = context.parse(InteractionType.class);
        long cooldown = (long) (context.parse(Double.class) * 1000D);
        long delay = (long) (context.parse(Integer.class) * 1D);
        String queue = context.dumpAllArgs();
        return new QueueAction(adventure, textSerializer, configManager, queue, type, cooldown, delay);
    }

    @Override
    public List<String> suggest(CommandContext context) throws CommandExecutionException {
        if (context.argSize() == 1) return context.suggestEnum(InteractionType.values());
        if (context.argSize() == 2) return context.suggestLiteral("1");
        if (context.argSize() == 3) return context.suggestLiteral("0");
        if (context.argSize() == 4) return context.suggestLiteral(PhoenixQueues.names().toArray(new String[0]));
        return Collections.emptyList();
    }

}
