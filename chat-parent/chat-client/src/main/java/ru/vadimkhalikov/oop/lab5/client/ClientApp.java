package ru.vadimkhalikov.oop.lab5.client;

import java.awt.Component;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;

import ru.vadimkhalikov.oop.lab5.common.Message;
import ru.vadimkhalikov.oop.lab5.common.protocol.MessageProtocol;
import ru.vadimkhalikov.oop.lab5.common.protocol.ProtocolFactory;
import ru.vadimkhalikov.oop.lab5.common.protocol.ProtocolType;

public class ClientApp {

    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private MessageProtocol protocol;
    private String username;

    private ChatWindow chatWindow;
    private LoginDialog loginDialog;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private volatile boolean explicitLogout = false;
    
    private static ProtocolType protocolType = ProtocolType.JAVA;
    private static final String clientId = UUID.randomUUID().toString().substring(0, 8);
    
    public static void setProtocol(String protocolCode) {
        try {
            ProtocolType newType = ProtocolType.fromString(protocolCode);
            protocolType = newType;
            System.out.println("Using protocol: " + protocolType);
            
            configureLogging();
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown protocol: " + protocolCode + ". Using default: " + protocolType);
        }
    }
    
    private static void configureLogging() {
        String logDir = "logs/client/" + protocolType.getCode();
        Path logPath = Paths.get(logDir);
        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            System.err.println("Could not create log directory: " + logDir);
        }
        
        System.setProperty("log.dir", logDir);
        System.setProperty("client.id", clientId);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                setProtocol(args[0]);
            } else {
                configureLogging();
            }
            
            FlatDarkLaf.setup();
            
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            
            UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(13f));
        } catch (Exception e) {
            System.err.println("Failed to set FlatDarkLaf Look and Feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            ClientApp client = new ClientApp();
            client.showLoginDialog();
        });
    }

    private void showLoginDialog() {
        if (loginDialog != null && loginDialog.isVisible()) {
            loginDialog.dispose();
        }
        loginDialog = new LoginDialog(this::attemptLogin); 
        loginDialog.setVisible(true);
    }

    private boolean attemptLogin(String server, String portStr, String user) {
        this.explicitLogout = false; 
        this.serverAddress = server;
        this.username = user;
        updateStatus("Connecting...", false);
        try {
            this.serverPort = Integer.parseInt(portStr);
            if (this.serverPort <= 0 || this.serverPort > 65535) {
                showLoginError("Invalid port number. Must be between 1 and 65535.");
                return false;
            }
        } catch (NumberFormatException e) {
            showLoginError("Invalid port number format.");
            return false;
        }

        if (username == null || username.trim().isEmpty()) {
            showLoginError("Username cannot be empty.");
            return false;
        }

        try {
            connectToServer();
            sendLoginRequest();
            startServerListener(); 
            return true;
        } catch (UnknownHostException e) {
            showLoginError("Unknown server host: " + serverAddress);
            updateStatus("Disconnected", false);
            return false;
        } catch (IOException e) {
            showLoginError("Connection error: " + e.getMessage());
            updateStatus("Disconnected", false);
            disconnect(); 
            return false;
        }
    }

    private void connectToServer() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        protocol = ProtocolFactory.createProtocol(protocolType);
        connected.set(true);
        System.out.println("Connected to server: " + serverAddress + ":" + serverPort);
    }

    private void sendLoginRequest() throws IOException {
        Message loginMsg = new Message(Message.MessageType.LOGIN_REQUEST, username);
        sendMessageInternal(loginMsg);
    }

    private void startServerListener() {
        new Thread(() -> {
            try {
                while (connected.get() && socket != null && !socket.isClosed() && socket.isConnected()) {
                    Message serverMessage = protocol.receiveMessage(inputStream);
                    handleServerMessage(serverMessage);
                }
            } catch (SocketException | EOFException e) {
                 if (!explicitLogout) {
                    System.err.println("Connection lost: " + e.getMessage());
                    handleConnectionLoss();
                 }
            } catch (IOException | ClassNotFoundException e) {
                 if (!explicitLogout) { 
                     System.err.println("Error reading from server: " + e.getMessage());
                     handleConnectionLoss();
                 }
            } finally {
                if (!explicitLogout && !connected.get()) { 
                     updateStatus("Disconnected", false);
                }
                System.out.println("ServerListenerThread finished.");
            }
        }, "ServerListenerThread").start();
    }
    
    private void handleConnectionLoss() {
        if (connected.compareAndSet(true, false)) {
            System.out.println("Handling connection loss...");
            closeStreamsAndSocket(); 
            updateStatus("Disconnected. Click Reconnect.", false);
        }
    }

    private void handleServerMessage(Message message) {
        SwingUtilities.invokeLater(() -> { 
            switch (message.getType()) {
                case LOGIN_SUCCESS:
                    System.out.println("Login successful!");
                    connected.set(true);
                    if (loginDialog != null) {
                        loginDialog.dispose(); 
                    }
                    if (chatWindow == null) {
                       openChatWindow();
                    } else {
                        updateStatus("Connected", true);
                    }
                    requestUserList();
                    break;
                case LOGIN_FAILURE:
                    showLoginError("Login failed: " + message.getContent());
                    updateStatus("Login failed", false);
                    disconnect();
                    break;
                case USER_MESSAGE:
                case SERVER_MESSAGE:
                case USER_JOINED:
                case USER_LEFT:
                    if (chatWindow != null) {
                        chatWindow.displayMessage(message);
                    }
                    break;
                case USER_LIST_RESPONSE:
                    if (chatWindow != null) {
                        chatWindow.updateUserList(message.getUserList());
                    }
                    break;
                default:
                    System.out.println("Received unknown message type: " + message.getType());
                    break;
            }
        });
    }

    private void openChatWindow() {
        chatWindow = new ChatWindow(this::sendMessage, this::requestUserList, this::sendLogoutRequest, this::reconnect);
        chatWindow.setVisible(true);
        updateStatus("Connected", true);
    }

    public void sendMessage(Message message) {
        if (!connected.get()) {
             showError("Not connected to server.");
             return;
        }
        try {
            sendMessageInternal(message);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            handleConnectionLoss();
        }
    }

    private void sendMessageInternal(Message message) throws IOException {
        if (outputStream != null && socket != null && !socket.isClosed()) { 
            message.setSender(this.username);
            synchronized (outputStream) { 
                protocol.sendMessage(message, outputStream);
            }
        } else {
            throw new IOException("Output stream/socket is not available.");
        }
    }

    private void requestUserList() {
        if (connected.get()) {
             sendMessage(new Message(Message.MessageType.USER_LIST_REQUEST));
        }
    }

    private void sendLogoutRequest() {
        explicitLogout = true;
        if (connected.get()) {
             try {
                 sendMessageInternal(new Message(Message.MessageType.LOGOUT_REQUEST));
             } catch (IOException e) {
                 System.err.println("Could not send logout message: " + e.getMessage());
             }
        }
        disconnect();
        // Полностью завершаем работу приложения
        System.exit(0);
    }
    
    public void reconnect() {
        if (connected.get()) {
            System.out.println("Already connected.");
            return;
        }
        System.out.println("Attempting to reconnect...");
        updateStatus("Reconnecting...", false);
        explicitLogout = false;
        
        closeStreamsAndSocket(); 

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                 try {
                     connectToServer();
                     sendLoginRequest(); 
                     startServerListener();
                     return true;
                 } catch (Exception e) {
                     System.err.println("Reconnect failed: " + e.getMessage());
                     closeStreamsAndSocket(); 
                     return false;
                 }
            }

            @Override
            protected void done() {
                 try {
                     boolean success = get();
                     if (!success) {
                         updateStatus("Reconnect failed. Click Reconnect.", false);
                     } else {
                         System.out.println("Reconnect attempt finished (waiting for LOGIN_SUCCESS).");
                     }
                 } catch (Exception e) {
                      System.err.println("Error during reconnect finalization: " + e.getMessage());
                      closeStreamsAndSocket();
                      updateStatus("Reconnect error. Click Reconnect.", false);
                 }
            }
        }.execute();
    }

    private void closeStreamsAndSocket() {
         connected.set(false);
         try {
             if (protocol != null) {
                 protocol.close();
                 protocol = null;
             }
         } catch (IOException e) {
             /* ignore */
         }
         
         try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
         } catch (IOException e) { /* ignore */ }
         
         try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
         } catch (IOException e) { /* ignore */ }
         
         try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                socket = null;
            }
         } catch (IOException e) { /* ignore */ }
    }

    public void disconnect() {
        System.out.println("Disconnecting fully...");
        closeStreamsAndSocket();
        username = null; 
        if (chatWindow != null) {
            SwingUtilities.invokeLater(() -> {
                if (chatWindow != null) { 
                    chatWindow.dispose();
                }
            });
            chatWindow = null;
        }
         System.out.println("Fully disconnected.");
    }

    private void updateStatus(String status, boolean isConnected) {
        connected.set(isConnected);
        if (chatWindow != null) {
            SwingUtilities.invokeLater(() -> chatWindow.updateConnectionStatus(status, isConnected));
        }
         if (loginDialog != null && loginDialog.isVisible()) {
             loginDialog.setStatus(status);
         }
    }

    private void showLoginError(String message) {
         if (loginDialog != null && loginDialog.isVisible()) {
             loginDialog.setStatus(message);
         } else {
             showError(message);
         }
         System.err.println("Login Error: " + message);
    }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
             Component parent = chatWindow;
             if (parent == null || !parent.isVisible()) {
                  parent = loginDialog;
             }
              if (parent == null || !parent.isVisible()) {
                   parent = null;
             }
            JOptionPane.showMessageDialog(parent,
                message, "Error", JOptionPane.ERROR_MESSAGE);
        });
        System.err.println("Error: " + message);
    }
} 