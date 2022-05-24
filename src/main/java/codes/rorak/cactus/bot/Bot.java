package codes.rorak.cactus.bot;

import codes.rorak.cactus.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.*;

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
    public String CURRENT_DIRECTORY = "~";
    private void listenForConsoleCommands() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String text = sc.nextLine();
            //command(text, CommandType.CONSOLE);
        }
    }
    private void serverCommand(Message msg) {
        String content = msg.getContentRaw().replaceAll("`", "").replaceAll("\n", "");
        MessageChannel channel = msg.getChannel();
        String author = msg.isFromType(ChannelType.TEXT) ? Objects.requireNonNull(msg.getMember()).getNickname() : msg.getAuthor().getName();
        if (msg.isFromType(ChannelType.TEXT)) msg.delete().queue();
        assert author != null;
        channel.sendMessage("┌──(**" + author + "**@**cactus**)-[*" + CURRENT_DIRECTORY + "*]\n└─`$ " + content + "`").queue();

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
                else if (quotes && !str.contains("\"")) arg.append(str);
                else if (!quotes && str.startsWith("\"") && str.endsWith("\"") && str.chars().filter(v->v=='"').count()==2) args.add(str.replaceAll("\"", ""));
                else if (!quotes && str.startsWith("\"") && str.chars().filter(v->v=='"').count()==1) {
                    quotes = true;
                    arg = new StringBuilder();
                    arg.append(str);
                }
                else if (quotes && str.endsWith("\"") && str.chars().filter(v->v=='"').count()==1) {
                    quotes = false;
                    arg.append(str);
                    args.add(arg.toString().replaceAll("\"", ""));
                }
                else {
                    BLog.err("Argument error: Invalid quotes here: '" + str.replaceAll("\"", "-> \"") + "'", channel);
                    return;
                }
            }
            if (quotes) {
                BLog.err("Argument error: Unclosed quotes!'", channel);
                return;
            }
        }
        onCommand(command, args.toArray(new String[0]), new BLog(channel)::log);
    }
    private void onCommand(String command, String[] args, LogMethod m) {
        m.test("Command: " + command + "\nArgs: " + String.join(" ;; ", args));
    }

    public static class BLog {
        private final MessageChannel channel;
        public BLog(MessageChannel c) {
            channel = c;
        }
        public static void err(String message, MessageChannel c) {
            c.sendMessage("**[ERROR]** `" + message + "`").queue();
        }
        public void log(String message) {
            log(message, channel);
        }
        public static void log(String message, MessageChannel c) {
            try {
                c.sendMessage("`" + message + "`").queue();
            }
            catch (Exception e) {
                Logger.err("Uncaught exception: ", e);
            }
        }
    }
    //endregion
}
