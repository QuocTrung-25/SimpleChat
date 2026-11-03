package simplechat1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientConsole implements ChatIF {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;

    private final ChatClient client;
    private String loginId;

    private ClientConsole(String loginId, String host, int port) {
        this.loginId = loginId;
        client = new ChatClient(this, host, port);
    }

    public static void main(String[] args) {
        if (args.length == 0 || args[0].trim().isEmpty()) {
            System.out.println("ERROR - No login ID specified. Connection aborted.");
            return;
        }

        if (args.length > 3) {
            System.out.println("Usage: java simplechat1.ClientConsole <loginID> [host] [port]");
            return;
        }

        String host = args.length > 1 ? args[1] : DEFAULT_HOST;
        int port;
        try {
            port = args.length > 2 ? parsePort(args[2]) : DEFAULT_PORT;
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR - " + e.getMessage());
            return;
        }

        new ClientConsole(args[0].trim(), host, port).run();
    }

    private void run() {
        connect();
        try (BufferedReader fromConsole =
                     new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = fromConsole.readLine()) != null) {
                if (!handleInput(input.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            display("Could not read from the console: " + e.getMessage());
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                display("Could not close the client connection: " + e.getMessage());
            }
        }
    }

    private void connect() {
        try {
            client.connect(loginId);
        } catch (IOException e) {
            display("Cannot open connection. Awaiting command.");
        }
    }

    private boolean handleInput(String input) {
        if (!input.startsWith("#")) {
            if (client.isChatConnected()) {
                client.sendMessage(input);
            } else {
                display("ERROR - Not connected. Use #login to connect.");
            }
            return true;
        }

        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length == 2 ? parts[1].trim() : "";

        switch (command) {
            case "#quit":
                disconnect();
                return false;
            case "#logoff":
                if (!client.isChatConnected()) {
                    display("ERROR - Client is not connected.");
                } else {
                    disconnect();
                }
                return true;
            case "#login":
                if (client.isChatConnected()) {
                    display("ERROR - Client is already connected.");
                } else {
                    if (!argument.isEmpty()) {
                        loginId = argument;
                    }
                    connect();
                }
                return true;
            case "#sethost":
                if (client.isChatConnected()) {
                    display("ERROR - Disconnect before changing the host.");
                } else if (argument.isEmpty()) {
                    display("ERROR - Usage: #sethost <host>");
                } else {
                    client.setHost(argument);
                    display("Host set to: " + argument);
                }
                return true;
            case "#setport":
                if (client.isChatConnected()) {
                    display("ERROR - Disconnect before changing the port.");
                } else {
                    try {
                        int port = parsePort(argument);
                        client.setPort(port);
                        display("port set to: " + port);
                    } catch (IllegalArgumentException e) {
                        display("ERROR - " + e.getMessage());
                    }
                }
                return true;
            case "#gethost":
                display("Current host: " + client.getHost());
                return true;
            case "#getport":
                display("Current port: " + client.getPort());
                return true;
            default:
                display("ERROR - Unknown command: " + command);
                return true;
        }
    }

    private void disconnect() {
        try {
            client.disconnect();
        } catch (IOException e) {
            display("Could not close the client connection: " + e.getMessage());
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
