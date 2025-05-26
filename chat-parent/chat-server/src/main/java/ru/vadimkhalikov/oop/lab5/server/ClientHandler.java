package ru.vadimkhalikov.oop.lab5.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.vadimkhalikov.oop.lab5.common.Message;
import ru.vadimkhalikov.oop.lab5.common.protocol.MessageProtocol;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private static final int SO_TIMEOUT_MS = 30000;

    private final Socket clientSocket;
    private final Server server;
    private final MessageProtocol protocol;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String username;

    public ClientHandler(Socket socket, Server server, MessageProtocol protocol) {
        this.clientSocket = socket;
        this.server = server;
        this.protocol = protocol;
        try {
            this.outputStream = clientSocket.getOutputStream();
            this.inputStream = clientSocket.getInputStream();
        } catch (IOException e) {
            log.error("Error creating streams for client {}: {}", socket.getRemoteSocketAddress(), e.getMessage());
            close();
        }
    }

    @Override
    public void run() {
        try {
            Message loginMessage = protocol.receiveMessage(inputStream);
            if (loginMessage.getType() == Message.MessageType.LOGIN_REQUEST) {
                handleLogin(loginMessage);
            } else {
                log.warn("Client {} sent invalid first message type: {}. Disconnecting.", clientSocket.getRemoteSocketAddress(), loginMessage.getType());
                sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Invalid login request"));
                server.removeClient(this);
                return;
            }

            while (clientSocket.isConnected() && !Thread.currentThread().isInterrupted()) {
                Message clientMessage = protocol.receiveMessage(inputStream);
                handleMessage(clientMessage);
            }

        } catch (SocketTimeoutException e) {
            log.warn("Client {} timed out. Disconnecting.", username != null ? username : clientSocket.getRemoteSocketAddress());
        } catch (SocketException e) {
            log.warn("Socket error for client {}: {}. Disconnecting.", username != null ? username : clientSocket.getRemoteSocketAddress(), e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            if (!clientSocket.isClosed()) {
                log.error("Error handling client {}: {}", username != null ? username : clientSocket.getRemoteSocketAddress(), e.getMessage());
            }
        } finally {
            server.removeClient(this);
        }
    }

    private void handleLogin(Message loginMessage) throws IOException {
        String requestedUsername = loginMessage.getSender();
        if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Username cannot be empty."));
            server.removeClient(this);
            log.warn("Login failed for {}: Empty username.", clientSocket.getRemoteSocketAddress());
            return;
        }
        if (server.isUsernameTaken(requestedUsername)) {
            sendMessage(new Message(Message.MessageType.LOGIN_FAILURE, "Username \"" + requestedUsername + "\" is already taken."));
            server.removeClient(this);
            log.warn("Login failed for {}: Username \"{}\" taken.", clientSocket.getRemoteSocketAddress(), requestedUsername);
            return;
        }

        this.username = requestedUsername;
        Message successMsg = new Message(Message.MessageType.LOGIN_SUCCESS);
        sendMessage(successMsg);
        log.info("Client {} logged in as {}.", clientSocket.getRemoteSocketAddress(), username);

        server.sendHistory(this);

        Message joinMsg = new Message(Message.MessageType.USER_JOINED, username, null);
        server.broadcastMessage(joinMsg, this);

        server.broadcastUserList();
    }

    private void handleMessage(Message message) throws IOException {
         log.debug("Received message from {}: {}", username, message.getType());
        switch (message.getType()) {
            case USER_MESSAGE:
                if (message.getContent() != null && !message.getContent().trim().isEmpty()) {
                    message.setSender(this.username);
                    server.broadcastMessage(message, this);
                    log.info("User [{}] sent message: {}", username, message.getContent());
                }
                break;
            case USER_LIST_REQUEST:
                 Message userListResponse = new Message(Message.MessageType.USER_LIST_RESPONSE);
                 userListResponse.setUserList(server.getUsernames());
                 sendMessage(userListResponse);
                 log.info("User [{}] requested user list.", username);
                break;
            case LOGOUT_REQUEST:
                log.info("User [{}] requested logout.", username);
                close();
                break;
            default:
                log.warn("Received unknown message type from {}: {}", username, message.getType());
                break;
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (outputStream != null && !clientSocket.isClosed()) {
            synchronized (outputStream) {
                protocol.sendMessage(message, outputStream);
            }
        }
    }

    public void close() {
        try {
            if (protocol != null) {
                protocol.close();
            }
        } catch (IOException e) {
            log.error("Error closing protocol for {}: {}", username, e.getMessage());
        }
        
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            log.error("Error closing input stream for {}: {}", username, e.getMessage());
        }
        
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
             log.error("Error closing output stream for {}: {}", username, e.getMessage());
        }
        
        try {
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
             log.error("Error closing client socket for {}: {}", username, e.getMessage());
        }
        
        log.debug("Closed resources for client handler {}", username);
    }

    public String getUsername() {
        return username;
    }
} 