package everyos.discord.bot.channelcase;

import java.util.HashMap;

import discord4j.core.object.entity.Message;
import everyos.discord.bot.command.CommandData;
import everyos.discord.bot.command.ICommand;
import everyos.discord.bot.command.IGroupCommand;
import everyos.discord.bot.command.utility.TicketManagerCommand;
import everyos.discord.bot.parser.ArgumentParser;
import reactor.core.publisher.Mono;

public class TicketChannelCase implements IGroupCommand {
	private HashMap<String, ICommand> commands;
    public TicketChannelCase() {
        commands = new HashMap<String, ICommand>();
        commands.put("ticket", new TicketManagerCommand());
    }
	
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
		String content = message.getContent().orElse("");
        String trunc = ArgumentParser.getIfPrefix(content, 
            new String[] {"--- ", "*", "<@"+data.bot.clientID+">", "<@!"+data.bot.clientID+">"});
        if (trunc == null) return Mono.empty();
        String command = ArgumentParser.getCommand(trunc);
        String arg = ArgumentParser.getArgument(trunc);

        if (commands.containsKey(command)) return commands.get(command).execute(message, data, arg);
        return Mono.empty();
	}
}
