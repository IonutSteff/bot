package everyos.discord.bot.command.utility;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import everyos.discord.bot.adapter.GuildAdapter;
import everyos.discord.bot.command.CommandData;
import everyos.discord.bot.command.ICommand;
import everyos.discord.bot.command.IGroupCommand;
import everyos.discord.bot.localization.Localization;
import everyos.discord.bot.localization.LocalizedString;
import everyos.discord.bot.parser.ArgumentParser;
import everyos.discord.bot.util.PermissionUtil;
import everyos.storage.database.DBArray;
import reactor.core.publisher.Mono;

public class AutoRoleCommand implements IGroupCommand {
	private HashMap<Localization, HashMap<String, ICommand>> lcommands;

	public AutoRoleCommand() {
	 	HashMap<String, ICommand> commands;
	    lcommands = new HashMap<Localization, HashMap<String, ICommand>>();

	    //Commands
	    ICommand roleAddCommand = new AutoRoleAddCommand();
	    ICommand roleRemoveCommand = new AutoRoleRemoveCommand();

	    //en_US
	    commands = new HashMap<String, ICommand>();
	    commands.put("add", roleAddCommand);
	    commands.put("remove", roleRemoveCommand);
	    lcommands.put(Localization.en_US, commands);
	}
	
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
		if (argument.equals("")) argument = "";

		String cmd = ArgumentParser.getCommand(argument);
		String arg = ArgumentParser.getArgument(argument);

		ICommand command = lcommands.get(data.locale.locale).get(cmd);

		if (command==null)
			return message.getChannel().flatMap(c->c.createMessage(data.locale.localize(LocalizedString.NoSuchSubcommand)));

		return command.execute(message, data, arg);
	}
}

class AutoRoleAddCommand implements ICommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
		return message.getChannel().flatMap(channel->{
			return message.getAuthorAsMember()
				.flatMap(m->PermissionUtil.check(m, channel, data.locale, Permission.MANAGE_ROLES))
				.flatMap(o->message.getGuild())
				.flatMap(guild->{
					ArgumentParser parser = new ArgumentParser(argument);
					
					if (!parser.couldBeRoleID()) return channel.createMessage(data.localize(LocalizedString.UnrecognizedUsage)).then(Mono.empty());
					
					AtomicReference<Mono<?>> completion = new AtomicReference<Mono<?>>();
					completion.set(channel.createMessage(data.localize(LocalizedString.RoleAdded)));
					
					GuildAdapter.of(data.shard, guild).getDocument().getObject((obj, doc)->{
						DBArray array = obj.getOrCreateArray("aroles", ()->new DBArray());
						if (array.getLength()>=3) {
							completion.set(channel.createMessage(data.localize(LocalizedString.TooManyRoles)));
							return;
						}
						
						array.add(parser.eatRoleID());
						
						doc.save();
					});
					
					return completion.get();
				});
		});
	}
}

class AutoRoleRemoveCommand implements ICommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
		return message.getChannel().flatMap(channel->{
			return message.getAuthorAsMember()
				.flatMap(m->PermissionUtil.check(m, channel, data.locale, Permission.MANAGE_ROLES))
				.flatMap(o->message.getGuild())
				.flatMap(guild->{
					ArgumentParser parser = new ArgumentParser(argument);
					
					if (!parser.couldBeRoleID()) return channel.createMessage(data.localize(LocalizedString.UnrecognizedUsage)).then(Mono.empty());
					
					GuildAdapter.of(data.shard, guild).getDocument().getObject((obj, doc)->{
						DBArray array = obj.getOrDefaultArray("aroles", new DBArray());
						array.removeFirst(parser.eatRoleID());
						
						doc.save();
					});
					
					return channel.createMessage(data.localize(LocalizedString.RoleRemoved));
				});
		});
	}
}