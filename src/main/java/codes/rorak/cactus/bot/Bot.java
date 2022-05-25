package codes.rorak.cactus.bot;

import codes.rorak.cactus.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static codes.rorak.cactus.Logger.err;
import static codes.rorak.cactus.Logger.log;

public class Bot extends ListenerAdapter {
    public static void start() {
        Thread th = new Thread(new Bot()::run);
        th.setName("Bot-Main");
        th.start();
    }
    private void run() {
        try {
            exec();
        }
        catch (Exception e) {
            if (e instanceof LoginException)
            {Logger.err("Cannot connect: " + e.getMessage());return;}
            Logger.err("Unknown bot exception!", e);
        }
    }

    public JDA me;
    private static final String TOKEN = Dotenv.load().get("TOKEN");
    private static final String CONSOLE_SERVER = Dotenv.load().get("CONSOLE_SERVER");
    private static final String BOT_OWNER = Dotenv.load().get("BOT_OWNER");
    private final Commands cmds = new Commands();
    private void exec() throws LoginException {
        me = JDABuilder.createDefault(TOKEN, GatewayIntent.getIntents(32750))
                .addEventListeners(this).build();
    }

    //region StartupEvents
    @Override public void onReady(@NotNull ReadyEvent ev) {
        log("Ready! Total guild count: " + ev.getGuildTotalCount() + ". Available: " + ev.getGuildAvailableCount());
    }
    @Override public void onResumed(@NotNull ResumedEvent ev) {
        log("Resumed!");
    }
    @Override public void onReconnected(@NotNull ReconnectedEvent ev) {
        log("Reconnected!");
    }
    @Override public void onDisconnect(@NotNull DisconnectEvent ev) {
        try {
            log("Disconnected!! Reason: " + (ev.isClosedByServer() ? "Closed by server!" : Objects.requireNonNull(ev.getCloseCode()).getMeaning()) +
                    (Objects.requireNonNull(ev.getCloseCode()).isReconnect() ? " Reconnecting..." : ""));
        } catch (Exception ex) {
            log("Disconnected!! Not closed by server, but CloseCode still null!!");
        }
    }
    @Override public void onShutdown(@NotNull ShutdownEvent ev) {
        log("Shut down! Code: " + ev.getCode());
    }
    @Override public void onException(@NotNull ExceptionEvent ev) {
        if (!ev.isLogged()) {
            Logger.err("A JDA error occurred!", ev.getCause());
        }
    }
    //endregion
    //region NormalEvents
    @Override public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) return;
        if ((e.isFromGuild() && e.getGuild().getId().equals(CONSOLE_SERVER))
                || (!e.isFromGuild() && e.getPrivateChannel().retrieveUser().complete().getId().equals(BOT_OWNER))) {
            serverCommand(e.getMessage());
            return;
        }

    }
    //endregion

    //region Command
    private Guild CURRENT_SERVER;
    private Channel SELECTED_CHANNEL;
    private Member SELECTED_MEMBER;
    private User SELECTED_USER;
    private Role SELECTED_ROLE;
    private Message SELECTED_MESSAGE;
    private @NotNull String CURRENT_DIRECTORY() {
        String str = "~";
        if (CURRENT_SERVER != null) str+="/"+CURRENT_SERVER.getName();
        if (SELECTED_USER != null) str+="/!"+SELECTED_USER.getAsTag();
        if (SELECTED_CHANNEL != null) str+="/#"+SELECTED_CHANNEL.getName();
        if (SELECTED_MEMBER != null) str+="/@"+SELECTED_MEMBER.getNickname()+"#"+SELECTED_MEMBER.getUser().getDiscriminator();
        if (SELECTED_ROLE != null) str+="/&"+SELECTED_ROLE.getName();
        if (SELECTED_MESSAGE != null) str+="/%"+SELECTED_MESSAGE.getId();
        return str;
    }

    private void listenForConsoleCommands() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String text = sc.nextLine();
            //command(text, CommandType.CONSOLE);
        }
    }
    private void serverCommand(@NotNull Message msg) {
        String content = msg.getContentRaw().replaceAll("`", "").replaceAll("\n", "");
        MessageChannel channel = msg.getChannel();
        String author = msg.isFromType(ChannelType.TEXT) ? Objects.requireNonNull(msg.getMember()).getNickname() : msg.getAuthor().getName();
        if (msg.isFromType(ChannelType.TEXT)) msg.delete().queue();
        assert author != null;
        channel.sendMessage("┌──(**" + author + "**@**cactus**)-[*" + CURRENT_DIRECTORY() + "*]\n└─`$ " + content + "`").complete();

        String[] split = content.split(" ");
        if (split.length < 1) {
            BLog.err("No command specified!", channel);
        }
        String command = split[0];
        List<String> args = new ArrayList<>();
        if (split.length > 1) {
            StringBuilder arg = new StringBuilder();
            boolean quotes = false;
            for (String str : Arrays.copyOfRange(split, 1, split.length)) {
                if (!quotes && !str.contains("\"")) args.add(str);
                else if (quotes && !str.contains("\"")) arg.append(" ").append(str);
                else if (!quotes && str.startsWith("\"") && str.endsWith("\"") && str.chars().filter(v->v=='"').count()==2) args.add(str.replaceAll("\"", ""));
                else if (!quotes && str.startsWith("\"") && str.chars().filter(v->v=='"').count()==1) {
                    quotes = true;
                    arg = new StringBuilder();
                    arg.append(str);
                }
                else if (quotes && str.endsWith("\"") && str.chars().filter(v->v=='"').count()==1) {
                    quotes = false;
                    arg.append(" ").append(str);
                    args.add(arg.toString().replaceAll("\"", ""));
                }
                else {
                    BLog.err("Argument error: Invalid quotes here: '" + str.replaceAll("\"", "-> \"") + "'", channel);
                    return;
                }
            }
            if (quotes) {
                BLog.err("Argument error: Unclosed quotes!", channel);
                return;
            }
        }
        BLog ll = new BLog(channel);
        onCommand(command, args.toArray(new String[0]), ll::log, ll::err, new CommandArgs(msg));
    }
    private void onCommand(String command, String[] args, LogMethod m, LogMethod err, CommandArgs cmd) {
        for (Method mt : Commands.class.getDeclaredMethods()) {
            if (mt.getName().equalsIgnoreCase(command)) {
                try {
                    mt.invoke(cmds, args, m, err, cmd);
                    return;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    err("Error - can't invoke " + mt.getName() + "()", e);
                }
            }
        }
    }

    public static class BLog {
        private final MessageChannel channel;
        public BLog(MessageChannel c) {
            channel = c;
        }
        public static void err(String message, MessageChannel c) {
            c.sendMessage("    **[ERROR]** `" + message + "`").queue();
        }
        public void log(String message) {
            log(message, channel);
        }
        public void err(String message) {err(message, channel);}
        public static void log(String message, MessageChannel c) {
            try {
                c.sendMessage("    `" + message + "`").queue();
            }
            catch (Exception e) {
                Logger.err("Uncaught exception: ", e);
            }
        }
    }
    public class Commands {
        @Used public void clear(String @NotNull [] args, LogMethod m, LogMethod err, CommandArgs cmd) {
            if (args.length == 0 && !cmd.isShell()) {
                cmd.getChannel().getIterableHistory().queue((l)-> cmd.getChannel().purgeMessages(l));
            }
        }
        @Used public void echo(String @NotNull [] args, LogMethod m, LogMethod err, CommandArgs cmd) {
            if (args.length == 0) {
                err.test("Echo: Invalid arguments! 'echo --help' for help.");
                return;
            }
            if (args[0].equals("--help")) {
                m.test("Usage: echo <message>");
                return;
            }
            m.test(String.join(" ", args));
        }
        @Used public void pwd(String[] args, @NotNull LogMethod m, LogMethod err, CommandArgs cmd) {
            String str = "Nowhere";
            if (CURRENT_SERVER != null)
                str = "SELECTED SERVER\nName: " + CURRENT_SERVER.getName() + "\nId: " + CURRENT_SERVER.getId() + "\nOwner: " + CURRENT_SERVER.retrieveOwner().complete().getUser().getAsTag();
            if (SELECTED_USER != null)
                str = "SELECTED USER\nName: " + SELECTED_USER.getAsTag() + "\nId: " + SELECTED_USER.getId();
            if (SELECTED_MEMBER != null)
                str += "SELECTED MEMBER\nName: " + SELECTED_MEMBER.getUser().getAsTag() + "\nNickname: " + SELECTED_MEMBER.getNickname() + "\nId: " + SELECTED_MEMBER.getId() + "\nRoles: " + String.join("\n    ", SELECTED_MEMBER.getRoles().stream().map(Role::getName).toList());
            if (SELECTED_ROLE != null)
                str += "SELECTED ROLE\nName: " + SELECTED_ROLE.getName() + "\nId: " + SELECTED_ROLE.getId() + "\nPermissions: " + String.join("\n    " + SELECTED_ROLE.getPermissions().stream().map(Permission::getName).toList());
            if (SELECTED_CHANNEL != null)
                str += "SELECTED CHANNEL\nName: " + SELECTED_CHANNEL.getName() + "\nId: " + SELECTED_CHANNEL.getId() + "\nType: " + SELECTED_CHANNEL.getType().name();
            if (SELECTED_MESSAGE != null)
                str += "SELECTED MESSAGE\nId: " + SELECTED_MESSAGE.getId() + "\nAuthor: " + SELECTED_MESSAGE.getAuthor() + "\nContent: " + SELECTED_MESSAGE.getContentRaw() + "\nFiles: " + (SELECTED_MESSAGE.getAttachments().size() != 0);
            m.test(str);
        }
    }
    //endregion
}
