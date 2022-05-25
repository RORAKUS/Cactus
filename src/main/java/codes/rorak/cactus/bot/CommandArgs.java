package codes.rorak.cactus.bot;

import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.Nullable;

public class CommandArgs {
    private boolean shell;
    private final MessageChannel channel;
    private final UserSnowflake author;
    private final Message commandMessage;

    public CommandArgs(MessageChannel ch, UserSnowflake user, Message command) {
        channel = ch;
        author = user;
        commandMessage = command;
        shell = false;
    }
    public CommandArgs(Message msg) {
        this(msg.getChannel(), (msg.isFromType(ChannelType.TEXT) ? msg.getMember() : msg.getAuthor()), msg);
    }
    public CommandArgs() {
        this(null, null, null);
        shell = true;
    }

    public MessageChannel getChannel() {return channel;}
    public @Nullable User getUser() {
        return (author instanceof User x) ? x : null;
    }
    public @Nullable Member getMember() {
        return (author instanceof  Member x) ? x : null;
    }
    public Message getMessage() {return commandMessage;}
    public boolean isServer() {return (author instanceof Member);}
    public boolean isShell() {return shell;}
}
