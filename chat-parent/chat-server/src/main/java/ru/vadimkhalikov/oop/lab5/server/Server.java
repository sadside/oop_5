package ru.vadimkhalikov.oop.lab5.server;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.vadimkhalikov.oop.lab5.common.Message;
import ru.vadimkhalikov.oop.lab5.common.protocol.MessageProtocol;
import ru.vadimkhalikov.oop.lab5.common.protocol.ProtocolFactory;
import ru.vadimkhalikov.oop.lab5.common.protocol.ProtocolType;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final String DEFAULT_CONFIG_FILE = "config/server.properties";
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_MAX_CLIENTS = 10;
    private static final String DEFAULT_LOGGING_ENABLED = "true";
    private static final int HISTORY_SIZE = 10;
    private static final ProtocolType DEFAULT_PROTOCOL = ProtocolType.JAVA;

    private final int port;
    private final boolean loggingEnabled;
    private final ProtocolType protocolType;
    private final ExecutorService clientPool;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());

    public Server() {
        Properties props = loadConfig();
        port = Integer.parseInt(props.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        loggingEnabled = Boolean.parseBoolean(props.getProperty("logging.enabled", DEFAULT_LOGGING_ENABLED));
        
        String protocolName = props.getProperty("server.protocol", DEFAULT_PROTOCOL.getCode());
        protocolType = ProtocolType.fromString(protocolName);
        
        int maxClients = Integer.parseInt(props.getProperty("server.maxclients", String.valueOf(DEFAULT_MAX_CLIENTS)));
        clientPool = Executors.newFixedThreadPool(maxClients);
        
        configureLogging();
        
        if (loggingEnabled) {
            log.info("Server configuration loaded: port={}, maxClients={}, protocol={}, loggingEnabled={}", 
                port, maxClients, protocolType, loggingEnabled);
        } else {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.OFF);
            System.out.println("Server configuration loaded: port=" + port + ", maxClients=" + maxClients + 
                ", protocol=" + protocolType + ", loggingEnabled=" + loggingEnabled);
        }
    }
    
    private void configureLogging() {
        String logDir = "logs/server/" + protocolType.getCode();
        Path logPath = Paths.get(logDir);
        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            System.err.println("Could not create log directory: " + logDir);
        }
        
        System.setProperty("log.dir", logDir);
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        
        String configPath = System.getProperty("server.config", DEFAULT_CONFIG_FILE);
        
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
            logInfo("Configuration loaded from '{}'", configPath);
        } catch (FileNotFoundException e) {
            logWarn("Configuration file '{}' not found. Using default settings.", configPath);
            createDefaultConfig(configPath);
        } catch (IOException e) {
            logError("Error reading configuration file '{}'. Using default settings.", configPath, e);
        }
        props.putIfAbsent("server.port", String.valueOf(DEFAULT_PORT));
        props.putIfAbsent("server.maxclients", String.valueOf(DEFAULT_MAX_CLIENTS));
        props.putIfAbsent("logging.enabled", DEFAULT_LOGGING_ENABLED);
        props.putIfAbsent("server.protocol", DEFAULT_PROTOCOL.getCode());
        return props;
    }

    private void createDefaultConfig(String configPath) {
        Properties props = new Properties();
        props.setProperty("server.port", String.valueOf(DEFAULT_PORT));
        props.setProperty("server.maxclients", String.valueOf(DEFAULT_MAX_CLIENTS));
        props.setProperty("logging.enabled", DEFAULT_LOGGING_ENABLED);
        props.setProperty("server.protocol", DEFAULT_PROTOCOL.getCode());
        
        // Создаем директорию для конфига, если она не существует
        Path configDir = Paths.get(configPath).getParent();
        if (configDir != null) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                logError("Could not create config directory: {}", configDir, e);
            }
        }
        
        try (OutputStream output = new FileOutputStream(configPath)) {
            props.store(output, "Server Configuration");
            logInfo("Created default configuration file: '{}'", configPath);
        } catch (IOException e) {
            logError("Error creating default configuration file '{}'", configPath, e);
        }
    }

    public void start() {
        logInfo("Server starting on port {} with protocol {}...", port, protocolType);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logInfo("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    
                    // Создаем протокол для нового клиента с использованием enum
                    MessageProtocol protocol = ProtocolFactory.createProtocol(protocolType);
                    
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this, protocol);
                    clients.add(clientHandler);
                    clientPool.execute(clientHandler);
                } catch (IOException e) {
                    logError("Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            logError("Server error: Could not listen on port " + port, e);
        } finally {
            clientPool.shutdown();
            logInfo("Server stopped.");
        }
    }

    public void broadcastMessage(Message message, ClientHandler senderHandler) {
        addMessageToHistory(message);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                boolean isOwnUserMessage = message.getType() == Message.MessageType.USER_MESSAGE && client == senderHandler;
                boolean isOwnEvent = (message.getType() == Message.MessageType.USER_JOINED || message.getType() == Message.MessageType.USER_LEFT)
                                      && message.getSender() != null && message.getSender().equals(client.getUsername());

                if (!isOwnUserMessage && !isOwnEvent) {
                   try {
                       client.sendMessage(message);
                   } catch (IOException e) {
                       logError("Error sending message to client {}: {}", client.getUsername(), e.getMessage());
                       removeClient(client);
                   }
                }
            }
        }
    }

    public void sendMessageToClient(Message message, ClientHandler recipient) {
         try {
            recipient.sendMessage(message);
         } catch (IOException e) { 
            logError("Error sending private message to client {}: {}", recipient.getUsername(), e.getMessage());
            removeClient(recipient);
         }
    }

    private void addMessageToHistory(Message message) {
        synchronized (messageHistory) {
            messageHistory.add(message);
        }
    }

    public void sendHistory(ClientHandler clientHandler) {
        synchronized (messageHistory) {
            int start = Math.max(0, messageHistory.size() - HISTORY_SIZE);
            for (int i = start; i < messageHistory.size(); i++) {
                try {
                    Message msg = messageHistory.get(i);
                     if (msg.getType() == Message.MessageType.USER_MESSAGE ||
                         msg.getType() == Message.MessageType.SERVER_MESSAGE ||
                         msg.getType() == Message.MessageType.USER_JOINED ||
                         msg.getType() == Message.MessageType.USER_LEFT) {
                        clientHandler.sendMessage(msg);
                     }
                } catch (IOException e) {
                    logError("Error sending history message to client {}: {}", clientHandler.getUsername(), e.getMessage());
                    removeClient(clientHandler);
                    break;
                }
            }
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        boolean removed = clients.remove(clientHandler);
        if (removed && clientHandler.getUsername() != null) {
            logInfo("Client {} disconnected.", clientHandler.getUsername());
            Message logoutMessage = new Message(Message.MessageType.USER_LEFT, clientHandler.getUsername(), null);
            broadcastMessage(logoutMessage, null);
            broadcastUserList();
        }
        clientHandler.close();
    }

    public List<String> getUsernames() {
        List<String> usernames = new ArrayList<>();
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername() != null) {
                    usernames.add(client.getUsername());
                }
            }
        }
        return usernames;
    }

    public void broadcastUserList() {
        List<String> usernames = getUsernames();
        Message userListMessage = new Message(Message.MessageType.USER_LIST_RESPONSE);
        userListMessage.setUserList(usernames);
        synchronized (clients) {
             for (ClientHandler client : clients) {
                 try {
                     client.sendMessage(userListMessage);
                 } catch (IOException e) {
                      logError("Error sending user list to client {}: {}", client.getUsername(), e.getMessage());
                      removeClient(client);
                 }
             }
        }
    }

    public boolean isUsernameTaken(String username) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername() != null && client.getUsername().equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void logInfo(String message, Object... args) {
        if (loggingEnabled) {
            log.info(message, args);
        } else {
            System.out.println("[INFO] " + String.format(message.replace("{}", "%s"), args));
        }
    }

     private void logWarn(String message, Object... args) {
        if (loggingEnabled) {
            log.warn(message, args);
        } else {
            System.out.println("[WARN] " + String.format(message.replace("{}", "%s"), args));
        }
    }

    private void logError(String message, Throwable throwable) {
        if (loggingEnabled) {
            log.error(message, throwable);
        } else {
            System.err.println("[ERROR] " + message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

     private void logError(String message, Object arg, Throwable throwable) {
        if (loggingEnabled) {
            log.error(message, arg, throwable);
        } else {
            System.err.println("[ERROR] " + String.format(message.replace("{}", "%s"), arg));
             if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    private void logError(String message, Object arg1, Object arg2) {
        if (loggingEnabled) {
            log.error(message, arg1, arg2);
        } else {
            String formattedMessage = message.replaceFirst("\\{}", String.valueOf(arg1));
            formattedMessage = formattedMessage.replaceFirst("\\{}", String.valueOf(arg2));
            System.err.println("[ERROR] " + formattedMessage);
             if (arg2 instanceof Throwable) {
                 ((Throwable) arg2).printStackTrace(System.err);
            } else if (arg1 instanceof Throwable) {
                 ((Throwable) arg1).printStackTrace(System.err);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
} 