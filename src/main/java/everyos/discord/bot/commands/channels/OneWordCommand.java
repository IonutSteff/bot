package everyos.discord.bot.commands.channels;

import java.util.HashMap;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import everyos.discord.bot.adapter.MessageAdapter;
import everyos.discord.bot.commands.ICommand;
import everyos.discord.bot.localization.Localization;
import everyos.discord.bot.localization.LocalizedString;
import everyos.discord.bot.object.CategoryEnum;
import everyos.storage.database.DBDocument;
import everyos.storage.database.DBObject;

public class OneWordCommand implements ICommand {
	@Override public void execute(Message message, MessageAdapter adapter, String argument) {
		adapter.getChannelAdapter(cadapter->{ //TODO: Check permissions
			adapter.getTextLocale(locale->{
                adapter.getMemberAdapter(madapter->{
                    madapter.hasPermission(Permission.MANAGE_CHANNELS, hp->{ //TODO: Check channel in same server
                        if (!hp) {
                            cadapter.send(adapter.formatTextLocale(locale, LocalizedString.InsufficientPermissions));
                            return;
                        }

                        cadapter.getGuildAdapter(gadapter->{
                            if (gadapter==null) {
                                return;
                            }
                            gadapter.createChannel("one-word", cadapter0->{ //TODO: Localize
                                if (cadapter0==null) return;

                                DBDocument cdoc = cadapter0.getDocument();
                                DBObject cobj = cdoc.getObject();
                                cobj.createObject("casedata", dbobj->{
                                    dbobj.set("sentence", "");
                                });
                                cobj.set("type", "oneword");
                                cdoc.save();
                                
                                cadapter.send(adapter.formatTextLocale(locale, LocalizedString.ChannelsSet));
                            });
                        });
                    });
                });
			});
		});
	}

	@Override public HashMap<String, ICommand> getSubcommands(Localization locale) { return null; }
	@Override public String getBasicUsage(Localization locale) { return null; }
	@Override public String getExtendedUsage(Localization locale) { return null; }
	
	@Override public CategoryEnum getCategory() {
		return CategoryEnum.Channels;
	}
}