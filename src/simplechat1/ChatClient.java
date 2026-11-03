package simplechat1;

import java.io.IOException;

import com.lloseng.ocsf.client.AbstractClient;

public class ChatClient extends AbstractClient {
    private static final String LOGIN_COMMAND = "#login ";
    private final ChatIF clientUI;
    private volatile boolean connected;

    public ChatClient(ChatIF clientUI, String host, int port) {
        super(host, port);
        this.clientUI = clientUI;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        clientUI.display("> " + msg);
    }

    @Override
    protected void connectionClosed() {
        connected = false;
        clientUI.display("Connection closed.");
    }

    @Override
    protected void connectionException(Exception exception) {
        clientUI.display("Server has shut down or the connection was interrupted: "
                + exception.getMessage());
    }

    public boolean isChatConnected() {
        return connected && isConnected();
    }

    public void connect(String loginId) throws IOException {
        openConnection();
        try {
            sendToServer(LOGIN_COMMAND + loginId);
            connected = true;
        } catch (IOException e) {
            connected = false;
            try {
                closeConnection();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    public void disconnect() throws IOException {
        if (!isConnected()) {
            connected = false;
            return;
        }

        closeConnection();
        long deadline = System.currentTimeMillis() + 3000;
        while (isConnected() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing the connection.", e);
            }
        }
        connected = false;
        if (isConnected()) {
            throw new IOException("Timed out while closing the connection.");
        }
    }

    public void sendMessage(String message) {
        try {
            sendToServer(message);
        } catch (IOException e) {
            clientUI.display("Could not send message: " + e.getMessage());
            try {
                disconnect();
            } catch (IOException closeError) {
                clientUI.display("Could not close the client connection: "
                        + closeError.getMessage());
            }
        }
    }
}
