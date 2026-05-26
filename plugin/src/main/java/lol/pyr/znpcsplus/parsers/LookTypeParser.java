package lol.pyr.znpcsplus.parsers;

import lol.pyr.director.adventure.command.CommandContext;
import lol.pyr.director.adventure.parse.ParserType;
import lol.pyr.director.common.command.CommandExecutionException;
import lol.pyr.director.common.message.Message;
import lol.pyr.znpcsplus.util.LookType;

import java.util.Deque;

public class LookTypeParser extends ParserType<LookType> {
    public LookTypeParser(Message<CommandContext> message) {
        super(message);
    }

    @Override
    public LookType parse(Deque<String> deque) throws CommandExecutionException {
        String input = deque.pop().toLowerCase().replace('-', '_');
        switch (input) {
            case "true":
            case "close":
            case "closest":
            case "closest_player":
                return LookType.CLOSEST_PLAYER;
            case "perplayer":
            case "per_player":
                return LookType.PER_PLAYER;
            case "false":
            case "fixed":
                return LookType.FIXED;
            default:
                throw new CommandExecutionException();
        }
    }
}
