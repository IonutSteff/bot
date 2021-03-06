package everyos.discord.bot.command.fun;

import java.awt.Color;
import java.util.HashMap;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import everyos.discord.bot.adapter.MusicAdapter;
import everyos.discord.bot.adapter.MusicAdapter.VoiceStateMissingException;
import everyos.discord.bot.annotation.Help;
import everyos.discord.bot.command.CategoryEnum;
import everyos.discord.bot.command.CommandData;
import everyos.discord.bot.command.ICommand;
import everyos.discord.bot.command.IGroupCommand;
import everyos.discord.bot.localization.Localization;
import everyos.discord.bot.localization.LocalizedString;
import everyos.discord.bot.parser.ArgumentParser;
import everyos.discord.bot.util.ErrorUtil.LocalizedException;
import everyos.discord.bot.util.MusicUtil;
import everyos.discord.bot.util.TimeUtil;
import reactor.core.publisher.Mono;

@Help(help=LocalizedString.MusicCommandHelp, ehelp = LocalizedString.MusicCommandExtendedHelp, category=CategoryEnum.Fun)
public class MusicCommand implements IGroupCommand {
	private HashMap<Localization, HashMap<String, ICommand>> lcommands;
	public MusicCommand() {
		HashMap<String, ICommand> commands;
        lcommands = new HashMap<Localization, HashMap<String, ICommand>>();

        //Commands
        ICommand musicPlayCommand = new MusicPlayCommand();
        ICommand musicStopCommand = new MusicStopCommand();
        ICommand musicSkipCommand = new MusicSkipCommand();
        ICommand musicShuffleCommand = new MusicShuffleCommand();
        ICommand musicPauseCommand = new MusicPauseCommand();
        ICommand musicUnpauseCommand = new MusicUnpauseCommand();
        ICommand musicNpCommand = new MusicNpCommand();
        ICommand musicRepeatCommand = new MusicRepeatCommand();
        ICommand musicQueueCommand = new MusicQueueCommand();
        ICommand musicPlaylistCommand = new MusicPlaylistCommand();
        ICommand musicRadioCommand = new MusicRadioCommand();

        //en_US
        commands = new HashMap<String, ICommand>();
        commands.put("play", musicPlayCommand);
        commands.put("stop", musicStopCommand);
        commands.put("skip", musicSkipCommand);
        commands.put("shuffle", musicShuffleCommand);
        commands.put("pause", musicPauseCommand);
        commands.put("unpause", musicUnpauseCommand);
        commands.put("np", musicNpCommand);
        commands.put("nowplaying", musicNpCommand);
        commands.put("repeat", musicRepeatCommand);
        commands.put("queue", musicQueueCommand);
        commands.put("playlist", musicPlaylistCommand);
        commands.put("radio", musicRadioCommand);
        lcommands.put(Localization.en_US, commands);
	}
	
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
        if (argument.equals(""))
        	return message.getChannel().flatMap(c->c.createMessage(data.localize(LocalizedString.NoSuchSubcommand)));

        String cmd = ArgumentParser.getCommand(argument);
        String arg = ArgumentParser.getArgument(argument);
        
        ICommand command = lcommands.get(data.locale.locale).get(cmd);
        
        if (command==null) return Mono.error(new LocalizedException(LocalizedString.NoSuchSubcommand));
	    
        return command.execute(message, data, arg);
    }

	@Override public HashMap<String, ICommand> getCommands(Localization locale) { return lcommands.get(locale); }
}

class MusicPlaylistCommand implements ICommand {
	private HashMap<Localization, HashMap<String, ICommand>> lcommands;
	public MusicPlaylistCommand() {
		HashMap<String, ICommand> commands;
        lcommands = new HashMap<Localization, HashMap<String, ICommand>>();

        //Commands

        //en_US
        commands = new HashMap<String, ICommand>();
        lcommands.put(Localization.en_US, commands);
	}
	
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
        if (argument.equals("")) return Mono.error(new LocalizedException(LocalizedString.NoSuchSubcommand));

        String cmd = ArgumentParser.getCommand(argument);
        String arg = ArgumentParser.getArgument(argument);
        
        ICommand command = lcommands.get(data.locale.locale).get(cmd);
        
        if (command==null) return Mono.error(new LocalizedException(LocalizedString.NoSuchSubcommand));
	    
        return command.execute(message, data, arg);
    }
}

abstract class GenericMusicCommand implements ICommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
			return message.getChannel().flatMap(channel->{
				return message.suppressEmbeds(true)
					.onErrorResume(e->Mono.empty())
					.then(message.getAuthorAsMember())
	            	.flatMap(m->MusicAdapter.getFromMember(data.bot, m))
	            	.flatMap(ma->execute(message, data, argument, ma, channel))
	            	.cast(Object.class)
	            	.onErrorResume(e->{
	            		if (e instanceof VoiceStateMissingException) {
	            			return channel.createMessage(data.localize(LocalizedString.NotInMusicChannel));
	            		}
	            		return Mono.error(e);
	            	});
	        });
			//TODO: We should scan out VoiceStates in other guilds if the current guild does not have a voice state
		}
	
	abstract Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel);
	
	public boolean requiresDJ() {return false;}
}

class MusicPlayCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		return MusicUtil.lookup(ma.getPlayer(), argument).flatMap(track->{
			return ma.queue(track, ma.getQueue().length).flatMap(o->channel.createEmbed(embed->{
				AudioTrackInfo info = track.getInfo();
				embed.setAuthor(data.safe(info.author), info.uri, null);
				embed.setTitle(data.safe(info.title));
                embed.setDescription("Song added to queue");
                embed.addField("Length", TimeUtil.formatTime(info.length), false);
                embed.setFooter("Requested by User ID "+message.getAuthor().get().getId().asLong(), null);
			}));
		}); 
	}
}

class MusicStopCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		ma.stop();
		return channel.createMessage(data.localize(LocalizedString.MusicStopped));
	}
}

class MusicSkipCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		if (argument.isEmpty()) {
			ma.skip(0);
		} else try {
			ma.skip(Integer.valueOf(argument));
		} catch (NumberFormatException e) {
			return Mono.error(new LocalizedException(LocalizedString.UnrecognizedUsage));
		}
		return channel.createMessage(data.localize(LocalizedString.TrackSkipped));
	}
}

class MusicShuffleCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		ma.shuffle();
		return channel.createMessage(data.localize(LocalizedString.QueueShuffled));
	}
}

class MusicPauseCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		ma.setPaused(true);
		return channel.createMessage(data.localize(LocalizedString.MusicPaused));
	}
}

class MusicUnpauseCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		ma.setPaused(false);
		return channel.createMessage(data.localize(LocalizedString.MusicUnpaused));
	}
}

class MusicNpCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		return showNowPlaying(data, ma, channel);
	}
	
	public static Mono<?> showNowPlaying(CommandData data, MusicAdapter ma, MessageChannel channel) {
		AudioTrack np = ma.getPlaying();
		if (np==null) return channel.createMessage(data.localize(LocalizedString.NoTrackPlaying));
        return channel.createEmbed(embed->{
            AudioTrackInfo info = np.getInfo();
            embed.setColor(Color.BLACK);
            embed.setAuthor(data.safe(info.author), info.uri, null);
            embed.setTitle(data.safe(info.title));
            embed.setDescription("Now playing");
            embed.addField("Length", 
                TimeUtil.formatTime(np.getPosition())+"/"+ TimeUtil.formatTime(info.length)+
                " ("+(Math.floor((((double) np.getPosition())/(double) info.length)*1000.)/10.)+"%)", false);
        });
	}
}

class MusicRepeatCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		ma.setRepeat(!ma.getRepeat());
		if (ma.getRepeat()) return channel.createMessage(data.localize(LocalizedString.MusicRepeatSet));
		return channel.createMessage(data.localize(LocalizedString.MusicRepeatUnset));
	}
}

class MusicQueueCommand extends GenericMusicCommand {
	@Override Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		AudioTrack[] queue = ma.getQueue();
		
		if (queue.length==0) return MusicNpCommand.showNowPlaying(data, ma, channel);
		
		return channel.createEmbed(embed->{
			embed.setTitle("Music Queue");
            Long ttime = 0L;
            
            {
            	AudioTrack track = ma.getPlaying();
                embed.addField("Now playing", "**Title:** "+track.getInfo().title+"\n"+
                    "**Length:** "+TimeUtil.formatTime(track.getDuration()), false);
                ttime+=track.getDuration();
            }
            for (int i=0; i<queue.length; i++) {
                AudioTrack track = queue[i];
                embed.addField("Track "+(i+1), "**Title:** "+track.getInfo().title+"\n"+
                    "**Length:** "+TimeUtil.formatTime(track.getDuration()), false);

                ttime+=track.getDuration();
            }
            embed.setFooter("Total length: "+TimeUtil.formatTime(ttime), null);
		});
	}
}

class MusicRadioCommand extends GenericMusicCommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument, MusicAdapter ma, MessageChannel channel) {
		return ma.setRadio(!ma.getRadio()).flatMap(r->{
			if (ma.getRadio()) return channel.createMessage(data.localize(LocalizedString.MusicRadioSet));
			return channel.createMessage(data.localize(LocalizedString.MusicRadioUnset));
		});
	}
}

class MusicPlayListAddCommand implements ICommand {
	@Override public Mono<?> execute(Message message, CommandData data, String argument) {
		return null;
	}
}

/*
public class MusicCommand implements ICommand {
	@Override public void execute(Message message, String argument) {
		//search, set time, volume, restart, trim
	    if (args[0].equals("playlist")) {
	        if (args.length<2) {
	            channel.send("Subcommand expected at least one argument!\n"+
	            "<playlist>[args] Subcommands on playlists include create, add, delete, and details\n"+
	            "Select playlist without subcommand to play it", true); return;
	        }
	        if (args.length==2) {
	            MusicObject music = getMusicChannel(guild, channel);
	            if (music==null) return;
	
	            PlaylistObject playlist = invoker.toGlobal().getPlaylist(args[1], false);
	            if (playlist == null) {
	                channel.send("Please create the playlist first!", true); return;
	            }
	
	            playlist.playlist.forEach(uri->playTrack(music, channel, uri));
	        } else if (args[2].equals("create")) {
	            invoker.toGlobal().getPlaylist(args[1], true);
	            channel.send("Created playlist!", true);
	        } else if (args[2].equals("delete")) {
	            invoker.toGlobal().playlists.remove(args[1]);
	            StaticFunctions.save();
	            channel.send("Deleted playlist!", true);
	        } else if (args[2].equals("add")) {
	            PlaylistObject playlist = invoker.toGlobal().getPlaylist(args[1], false);
	            if (playlist == null) {
	                channel.send("Please create the playlist first!", true); return;
	            }
	            if (args.length<4) {
	                channel.send("Expected URL!", true); return;
	            }
	            synchronized(playlist.playlist) {
	                playlist.playlist.add(args[3]);
	            }
	            StaticFunctions.save();
	            channel.send("Added to playlist!", true);
	        } else if (args[2].equals("details")) {
	            PlaylistObject playlist = invoker.toGlobal().getPlaylist(args[1], false);
	            if (playlist == null) {
	                channel.send("No such playlist!", true); return;
	            }
	            channel.send(embed->{
	                embed.setTitle("Playlist");
	                synchronized(playlist.playlist) {
	                    for (int i=0; i<playlist.playlist.size(); i++) {
	                        String track = playlist.playlist.get(i);
	                        embed.addField("Track "+(i+1), "**Title:** "+MessageHelper.filter(track), false);
	                    }
	                }
	            });
	        }
	    }
	}
}
*/