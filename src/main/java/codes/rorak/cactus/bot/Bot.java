package codes.rorak.cactus.bot;

import codes.rorak.cactus.Logger;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.Objects;

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

    private JDA me;
    private static final String TOKEN = Dotenv.configure().directory("/src/main/resources").load().get("TOKEN");
    private void exec() throws LoginException {
        me = JDABuilder.createDefault(TOKEN, GatewayIntent.getIntents(32750))
                .addEventListeners(this).build();
    }

    //region StartupEvents
    @Override public void onReady(@NotNull ReadyEvent ev) {
        Logger.log("Ready! Total guild count: " + ev.getGuildTotalCount() + ". Available: " + ev.getGuildAvailableCount());
    }
    @Override public void onResumed(@NotNull ResumedEvent ev) {
        Logger.log("Resumed!");
    }
    @Override public void onReconnected(@NotNull ReconnectedEvent ev) {
        Logger.log("Reconnected!");
    }
    @Override public void onDisconnect(@NotNull DisconnectEvent ev) {
        try {
            Logger.log("Disconnected!! Reason: " + (ev.isClosedByServer() ? "Closed by server!" : Objects.requireNonNull(ev.getCloseCode()).getMeaning()) +
                    (Objects.requireNonNull(ev.getCloseCode()).isReconnect() ? " Reconnecting..." : ""));
        } catch (Exception ex) {
            Logger.log("Disconnected!! Not closed by server, but CloseCode still null!!");
        }
    }
    @Override public void onShutdown(@NotNull ShutdownEvent ev) {
        Logger.log("Shut down! Code: " + ev.getCode());
    }
    @Override public void onException(@NotNull ExceptionEvent ev) {
        if (!ev.isLogged()) {
            Logger.err("A JDA error occurred!", ev.getCause());
        }
    }
    //endregion
}
