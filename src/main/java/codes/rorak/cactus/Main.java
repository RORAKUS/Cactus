package codes.rorak.cactus;

import codes.rorak.cactus.bot.Bot;

public class Main {
    public static void main(String[] args) {
        Logger.init();
        WebServer.start();
        Bot.start();
    }
}
