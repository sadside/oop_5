package ru.vadimkhalikov.oop.lab5.common.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import ru.vadimkhalikov.oop.lab5.common.Message;

/**
 * Реализация протокола, использующая JSON для сериализации сообщений.
 * Этот протокол может использоваться как для REST API, так и для WebSocket.
 */
public class RestJsonProtocol implements MessageProtocol {
    
    private final ObjectMapper objectMapper;
    
    public RestJsonProtocol() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void sendMessage(Message message, OutputStream out) throws IOException {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(message);
        
        // Отправляем 4 байта с длиной сообщения, за которыми следует JSON
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(jsonBytes.length);
        out.write(buffer.array());
        out.write(jsonBytes);
        out.flush();
    }
    
    @Override
    public Message receiveMessage(InputStream in) throws IOException {
        // Считываем 4 байта, чтобы определить длину сообщения
        byte[] lengthBytes = new byte[4];
        if (in.read(lengthBytes) != 4) {
            throw new IOException("Failed to read message length");
        }
        
        int messageLength = ByteBuffer.wrap(lengthBytes).getInt();
        byte[] messageBytes = new byte[messageLength];
        
        int bytesRead = 0;
        while (bytesRead < messageLength) {
            int count = in.read(messageBytes, bytesRead, messageLength - bytesRead);
            if (count < 0) {
                throw new IOException("End of stream reached before message was fully read");
            }
            bytesRead += count;
        }
        
        return objectMapper.readValue(messageBytes, Message.class);
    }
    
    @Override
    public String getProtocolName() {
        return "rest-json";
    }
    
    @Override
    public void close() throws IOException {
        // Нет ресурсов для освобождения
    }
    
    /**
     * Вспомогательный метод для преобразования объекта Message в JSON строку
     * 
     * @param message объект сообщения
     * @return строка JSON
     * @throws IOException при ошибке сериализации
     */
    public String messageToJson(Message message) throws IOException {
        return objectMapper.writeValueAsString(message);
    }
    
    /**
     * Вспомогательный метод для преобразования JSON строки в объект Message
     * 
     * @param json строка JSON
     * @return объект сообщения
     * @throws IOException при ошибке десериализации
     */
    public Message jsonToMessage(String json) throws IOException {
        return objectMapper.readValue(json, Message.class);
    }
} 