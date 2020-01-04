package everyos.discord.exobot.commands;

import discord4j.core.object.entity.Message;
import everyos.discord.exobot.objects.ChannelObject;
import everyos.discord.exobot.objects.GuildObject;
import everyos.discord.exobot.objects.UserObject;
import everyos.discord.exobot.util.ChannelHelper;
import everyos.discord.exobot.util.GuildHelper;
import everyos.discord.exobot.util.StringUtil;
import everyos.discord.exobot.util.UserHelper;

public class KickCommand implements ICommand {
	@Override public void execute(Message message, String argument) {
		GuildObject guild = GuildHelper.getGuildData(message.getGuild());
		ChannelObject channel = ChannelHelper.getChannelData(guild, message.getChannel().block());
		if (!UserHelper.getUserData(guild, message.getAuthorAsMember()).isOpted()) {
			channel.send("User is not opted to use this command", true); return;
		}
		
		String[] args = StringUtil.split(argument, " ");
		if (args.length==0) {
			channel.send("Expected one parameter", true); return;
		}
		
		UserObject user = UserHelper.getUserData(guild, UserHelper.parseUserId(args[0]));
		UserObject invoker = UserHelper.getUserData(guild, message.getAuthorAsMember());
		if (!invoker.isHigherThan(user)) {
			channel.send("Cannot kick: Invoker's role is not above specified user's", true); return;
		}
		
		StringBuilder reason = new StringBuilder();;
		if (args.length>1)
			for (int i=1; i<args.length; i++)
				reason.append(args[i]);
				
		user.kick(reason.toString());
		
		channel.send("User has been kicked", true);
	}

	@Override public String getHelp() {
		return "<user>[reason] Kicks specified user //TODO: Mod-logs channel";
	}

	@Override public COMMANDS getType() {
		return COMMANDS.Moderation;
	}
	
	@Override public String getFullHelp() {
		return "**<user>** The user to kick\n**[reason]** The reason for the kick";
	}
}
