package simplechat1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerConsole implements ChatIF {
    private static final int DEFAULT_PORT = 5555;
    private final EchoServer server;

    private ServerConsole(int port) {
        server = new EchoServer(port, this);
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: java simplechat1.ServerConsole [port]");
            return;
        }

        int port;
        try {
            port = args.length == 1 ? parsePort(args[0]) : DEFAULT_PORT;
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR - " + e.getMessage());
            return;
        }

        ServerConsole console = new ServerConsole(port);
        console.run();
    }

    private void run() {
        try {
            server.listen();
        } catch (IOException e) {
            display("Could not listen on port " + server.getPort() + ": " + e.getMessage());
            return;
        }

        try (BufferedReader fromConsole =
                     new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = fromConsole.readLine()) != null) {
                server.handleMessageFromServerUI(input);
                if (input.trim().equalsIgnoreCase("#quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            display("Could not read from the console: " + e.getMessage());
        } finally {
            try {
                if (!server.isClosed()) {
                    server.close();
                }
            } catch (IOException e) {
                display("Could not close the server: " + e.getMessage());
            }
        }
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535.");
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be a number.", e);
        }
    }

    @Override
    public void display(String message) {
        System.out.println(message);
    }
}
