package ru.vadimkhalikov.oop.lab5.common.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.vadimkhalikov.oop.lab5.common.Message;

/**
 * Реализация протокола, использующая XML-сообщения.
 * Формат: 4-байтовый заголовок с длиной сообщения, затем само XML-сообщение.
 */
public class XmlProtocol implements MessageProtocol {

    private static final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    
    private static final ThreadLocal<String> sessionIdHolder = ThreadLocal.withInitial(() -> "");

    @Override
    public void sendMessage(Message message, OutputStream out) throws IOException {
        try {
            String xmlString = messageToXml(message);
            
            byte[] xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
            
            ByteBuffer lengthHeader = ByteBuffer.allocate(4);
            lengthHeader.putInt(xmlBytes.length);
            
            synchronized (out) {
                out.write(lengthHeader.array());
                out.write(xmlBytes);
                out.flush();
            }
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("Error creating XML message", e);
        }
    }

    @Override
    public Message receiveMessage(InputStream in) throws IOException, ClassNotFoundException {
        try {
            byte[] lengthHeader = new byte[4];
            int bytesRead = in.read(lengthHeader);
            if (bytesRead != 4) {
                throw new IOException("Failed to read message length header");
            }
            
            int messageLength = ByteBuffer.wrap(lengthHeader).getInt();
            if (messageLength <= 0 || messageLength > 1_000_000) {
                throw new IOException("Invalid message length: " + messageLength);
            }
            
            byte[] messageBytes = new byte[messageLength];
            int totalBytesRead = 0;
            while (totalBytesRead < messageLength) {
                int bytesReadNow = in.read(messageBytes, totalBytesRead, messageLength - totalBytesRead);
                if (bytesReadNow == -1) {
                    throw new IOException("End of stream reached before message was fully read");
                }
                totalBytesRead += bytesReadNow;
            }
            
            String xmlString = new String(messageBytes, StandardCharsets.UTF_8);
            
            return xmlToMessage(xmlString);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Error parsing XML message", e);
        }
    }

    @Override
    public String getProtocolName() {
        return "xml";
    }

    @Override
    public void close() throws IOException {
        sessionIdHolder.remove();
    }

    /**
     * Сохраняет ID сессии, полученный от сервера
     */
    public void setSessionId(String sessionId) {
        sessionIdHolder.set(sessionId);
    }

    /**
     * Возвращает текущий ID сессии
     */
    public String getSessionId() {
        return sessionIdHolder.get();
    }

    /**
     * Преобразует объект Message в XML-строку согласно протоколу
     */
    private String messageToXml(Message message) throws ParserConfigurationException, TransformerException {
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement;
        
        // Получаем sessionId из ThreadLocal
        String sessionId = sessionIdHolder.get();
        
        switch (message.getType()) {
            case LOGIN_REQUEST:
                // <command name="login"><name>USER_NAME</name><type>CHAT_CLIENT_NAME</type></command>
                rootElement = doc.createElement("command");
                rootElement.setAttribute("name", "login");
                
                Element nameElement = doc.createElement("name");
                nameElement.setTextContent(message.getSender());
                rootElement.appendChild(nameElement);
                
                Element typeElement = doc.createElement("type");
                typeElement.setTextContent("JavaChatClient");
                rootElement.appendChild(typeElement);
                break;
                
            case LOGIN_SUCCESS:
                // <success><session>UNIQUE_SESSION_ID</session></success>
                rootElement = doc.createElement("success");
                
                Element sessionElement = doc.createElement("session");
                // Если ID сессии был в сообщении - используем его
                String messageSessionId = message.getContent();
                if (messageSessionId == null || messageSessionId.isEmpty()) {
                    messageSessionId = UUID.randomUUID().toString();
                }
                sessionElement.setTextContent(messageSessionId);
                rootElement.appendChild(sessionElement);
                break;
                
            case LOGIN_FAILURE:
                // <error><message>REASON</message></error>
                rootElement = doc.createElement("error");
                
                Element errorMsgElement = doc.createElement("message");
                errorMsgElement.setTextContent(message.getContent());
                rootElement.appendChild(errorMsgElement);
                break;
                
            case USER_LIST_REQUEST:
                // <command name="list"><session>UNIQUE_SESSION_ID</session></command>
                rootElement = doc.createElement("command");
                rootElement.setAttribute("name", "list");
                
                Element listSessionElement = doc.createElement("session");
                listSessionElement.setTextContent(sessionId);
                rootElement.appendChild(listSessionElement);
                break;
                
            case USER_LIST_RESPONSE:
                // <success><listusers>...</listusers></success>
                rootElement = doc.createElement("success");
                
                Element listUsersElement = doc.createElement("listusers");
                
                if (message.getUserList() != null) {
                    for (String user : message.getUserList()) {
                        Element userElement = doc.createElement("user");
                        
                        Element userNameElement = doc.createElement("name");
                        userNameElement.setTextContent(user);
                        userElement.appendChild(userNameElement);
                        
                        Element userTypeElement = doc.createElement("type");
                        userTypeElement.setTextContent("JavaChatClient");
                        userElement.appendChild(userTypeElement);
                        
                        listUsersElement.appendChild(userElement);
                    }
                }
                
                rootElement.appendChild(listUsersElement);
                break;
                
            case USER_MESSAGE:
                // <command name="message"><message>MESSAGE</message><session>UNIQUE_SESSION_ID</session></command>
                rootElement = doc.createElement("command");
                rootElement.setAttribute("name", "message");
                
                Element msgElement = doc.createElement("message");
                msgElement.setTextContent(message.getContent());
                rootElement.appendChild(msgElement);
                
                Element msgSessionElement = doc.createElement("session");
                msgSessionElement.setTextContent(sessionId);
                rootElement.appendChild(msgSessionElement);
                break;
                
            case SERVER_MESSAGE:
                // <event name="message"><message>MESSAGE</message><name>CHAT_NAME_FROM</name></event>
                rootElement = doc.createElement("event");
                rootElement.setAttribute("name", "message");
                
                Element serverMsgElement = doc.createElement("message");
                serverMsgElement.setTextContent(message.getContent());
                rootElement.appendChild(serverMsgElement);
                
                Element fromElement = doc.createElement("name");
                fromElement.setTextContent(message.getSender());
                rootElement.appendChild(fromElement);
                break;
                
            case USER_JOINED:
                // <event name="userlogin"><name>USER_NAME</name></event>
                rootElement = doc.createElement("event");
                rootElement.setAttribute("name", "userlogin");
                
                Element joinedNameElement = doc.createElement("name");
                joinedNameElement.setTextContent(message.getSender());
                rootElement.appendChild(joinedNameElement);
                break;
                
            case USER_LEFT:
                // <event name="userlogout"><name>USER_NAME</name></event>
                rootElement = doc.createElement("event");
                rootElement.setAttribute("name", "userlogout");
                
                Element leftNameElement = doc.createElement("name");
                leftNameElement.setTextContent(message.getSender());
                rootElement.appendChild(leftNameElement);
                break;
                
            case LOGOUT_REQUEST:
                // <command name="logout"><session>UNIQUE_SESSION_ID</session></command>
                rootElement = doc.createElement("command");
                rootElement.setAttribute("name", "logout");
                
                Element logoutSessionElement = doc.createElement("session");
                logoutSessionElement.setTextContent(sessionId);
                rootElement.appendChild(logoutSessionElement);
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported message type for XML protocol: " + message.getType());
        }
        
        doc.appendChild(rootElement);
        
        // Преобразование XML-документа в строку
        StringWriter writer = new StringWriter();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        
        return writer.toString();
    }

    /**
     * Преобразует XML-строку в объект Message согласно протоколу
     */
    private Message xmlToMessage(String xmlString) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(new InputSource(new StringReader(xmlString)));
        doc.getDocumentElement().normalize();
        
        Element rootElement = doc.getDocumentElement();
        String rootName = rootElement.getNodeName();
        
        if ("command".equals(rootName)) {
            String commandName = rootElement.getAttribute("name");
            
            if ("login".equals(commandName)) {
                String username = getElementContent(rootElement, "name");
                return new Message(Message.MessageType.LOGIN_REQUEST, username, null);
            } else if ("list".equals(commandName)) {
                return new Message(Message.MessageType.USER_LIST_REQUEST);
            } else if ("message".equals(commandName)) {
                String content = getElementContent(rootElement, "message");
                return new Message(Message.MessageType.USER_MESSAGE, content);
            } else if ("logout".equals(commandName)) {
                return new Message(Message.MessageType.LOGOUT_REQUEST);
            }
        } else if ("success".equals(rootName)) {
            NodeList sessionNodes = rootElement.getElementsByTagName("session");
            if (sessionNodes.getLength() > 0) {
                String sessionId = sessionNodes.item(0).getTextContent();
                sessionIdHolder.set(sessionId);
                return new Message(Message.MessageType.LOGIN_SUCCESS, sessionId);
            }
            
            NodeList listUsersNodes = rootElement.getElementsByTagName("listusers");
            if (listUsersNodes.getLength() > 0) {
                List<String> userList = new ArrayList<>();
                NodeList userNodes = ((Element)listUsersNodes.item(0)).getElementsByTagName("user");
                
                for (int i = 0; i < userNodes.getLength(); i++) {
                    Element userElement = (Element) userNodes.item(i);
                    NodeList nameNodes = userElement.getElementsByTagName("name");
                    if (nameNodes.getLength() > 0) {
                        userList.add(nameNodes.item(0).getTextContent());
                    }
                }
                
                Message userListMsg = new Message(Message.MessageType.USER_LIST_RESPONSE);
                userListMsg.setUserList(userList);
                return userListMsg;
            }
            
            return new Message(Message.MessageType.LOGIN_SUCCESS);
        } else if ("error".equals(rootName)) {
            String errorMsg = getElementContent(rootElement, "message");
            return new Message(Message.MessageType.LOGIN_FAILURE, errorMsg);
        } else if ("event".equals(rootName)) {
            String eventName = rootElement.getAttribute("name");
            
            if ("message".equals(eventName)) {
                String content = getElementContent(rootElement, "message");
                String sender = getElementContent(rootElement, "name");
                return new Message(Message.MessageType.SERVER_MESSAGE, sender, content);
            } else if ("userlogin".equals(eventName)) {
                String username = getElementContent(rootElement, "name");
                return new Message(Message.MessageType.USER_JOINED, username, null);
            } else if ("userlogout".equals(eventName)) {
                String username = getElementContent(rootElement, "name");
                return new Message(Message.MessageType.USER_LEFT, username, null);
            }
        }
        
        throw new IOException("Unknown XML message format: " + xmlString);
    }

    /**
     * Получает содержимое первого дочернего элемента с заданным именем
     */
    private String getElementContent(Element parentElement, String childName) {
        NodeList nodes = parentElement.getElementsByTagName(childName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
} 