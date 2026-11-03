package simplechat1;

import java.io.IOException;
import com.lloseng.ocsf.server.AbstractServer;
import com.lloseng.ocsf.server.ConnectionToClient;

public class EchoServer extends AbstractServer implements ChatIF {
    private static final int DEFAULT_PORT = 5555;
    private static final String LOGIN_ID = "loginID";
    private final ChatIF serverUI;
    private volatile boolean stopRequested;

    public EchoServer(int port) {
        this(port, message -> System.out.println(message));
    }

    public EchoServer(int port, ChatIF serverUI) {
        super(port);
        this.serverUI = serverUI;
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        Object loginInfo = client.getInfo(LOGIN_ID);
        String label = loginInfo == null ? "Message received " : "Message received: ";
        serverUI.display(label + msg + " from " + loginInfo);

        if (!(msg instanceof String)) {
            rejectClient(client, "ERROR - Only text messages are supported.");
            return;
        }

        String message = (String) msg;
        if (loginInfo == null) {
            handleLogin(message, client);
            return;
        }

        if (message.startsWith("#login")) {
            rejectClient(client, "ERROR - Login is only allowed as the first command.");
            return;
        }

        sendToAllClients(loginInfo + "> " + message);
    }

    private void handleLogin(String message, ConnectionToClient client) {
        if (!message.startsWith("#login ")) {
            rejectClient(client, "ERROR - Login must be the first command.");
            return;
        }

        String loginId = message.substring("#login ".length()).trim();
        if (loginId.isEmpty() || loginId.startsWith("#")) {
            rejectClient(client, "ERROR - A login ID is required.");
            return;
        }

        client.setInfo(LOGIN_ID, loginId);
        String announcement = loginId + " has logged on.";
        serverUI.display(announcement);
        sendToAllClients(announcement);
    }

    private void rejectClient(ConnectionToClient client, String message) {
        try {
            client.sendToClient(message);
            client.close();
        } catch (IOException e) {
            serverUI.display("Could not reject client connection: " + e.getMessage());
        }
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        serverUI.display("A new client is attempting to connect to the server.");
    }

    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        Object loginInfo = client.getInfo(LOGIN_ID);
        if (loginInfo != null) {
            String message = loginInfo + " has disconnected.";
            serverUI.display(message);
            sendToAllClients(message);
        }
    }

    @Override
    protected void serverStarted() {
        stopRequested = false;
        serverUI.display("Server listening for connections on port " + getPort());
    }

    @Override
    protected void serverStopped() {
        serverUI.display("Server has stopped listening for connections.");
    }

    @Override
    protected void serverClosed() {
        serverUI.display("Server is closed.");
    }

    @Override
    public void display(String message) {
        serverUI.display(message);
    }

    public void handleMessageFromServerUI(String message) {
        String normalized = message.trim();
        if (normalized.isEmpty()) {
            return;
        }

        if (!normalized.startsWith("#")) {
            String serverMessage = "SERVER MESSAGE> " + normalized;
            serverUI.display(serverMessage);
            sendToAllClients(serverMessage);
            return;
        }

        String[] parts = normalized.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length == 2 ? parts[1].trim() : "";
        try {
            switch (command) {
                case "#quit":
                    close();
                    break;
                case "#stop":
                    if (isClosed() || !isListening()) {
                        serverUI.display("ERROR - Server is not listening.");
                    } else {
                        stopRequested = true;
                        sendToAllClients("WARNING - Server has stopped listening for connections.");
                        stopListening();
                    }
                    break;
                case "#close":
                    if (isClosed()) {
                        serverUI.display("ERROR - Server is already closed.");
                    } else {
                        close();
                    }
                    break;
                case "#setport":
                    if (!isClosed()) {
                        serverUI.display("ERROR - Close the server before changing the port.");
                    } else {
                        int newPort = parsePort(argument);
                        setPort(newPort);
                        serverUI.display("port set to: " + newPort);
                    }
                    break;
                case "#start":
                    if (isListening() && !stopRequested) {
                        serverUI.display("ERROR - Server is already listening.");
                    } else {
                        long deadline = System.currentTimeMillis() + 3000;
                        while (isListening() && System.currentTimeMillis() < deadline) {
                            Thread.sleep(10);
                        }
                        if (isListening()) {
                            serverUI.display("ERROR - Server has not stopped listening yet.");
                            break;
                        }
                        listen();
                    }
                    break;
                case "#getport":
                    serverUI.display("Current port: " + getPort());
                    break;
                default:
                    serverUI.display("ERROR - Unknown command: " + command);
                    break;
            }
        } catch (IOException e) {
            serverUI.display("Server command failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            serverUI.display("Server command interrupted.");
        } catch (IllegalArgumentException e) {
            serverUI.display("Server command failed: " + e.getMessage());
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
            throw new IllegalArgumentException("Usage: #setport <port>", e);
        }
    }

    public static void main(String[] args) {
        ServerConsole.main(args);
    }
}
