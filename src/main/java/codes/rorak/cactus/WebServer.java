package codes.rorak.cactus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class WebServer implements HttpHandler {
    public static void start() {
        Thread th = new Thread(new WebServer()::exec);
        th.setName("WebServer");
        th.start();
    }
    private void run() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", this);
        server.setExecutor(null);
        server.start();
    }
    private void exec() {
        try {
            run();
        } catch (IOException e) {
            Logger.err("Web server error!", e);
        }
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String html = """
                <!DOCTYPE html>
                <html>
                    <head>
                        <title>Cache web</title>
                    </head>
                    <body>
                        <h1>You can close this</h1>
                        <!-- The question is - shall you? -->
                    </body>
                </html>
                """;
        httpExchange.sendResponseHeaders(200, html.length());
        OutputStream os = httpExchange.getResponseBody();
        os.write(html.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
