package ru.vadimkhalikov.oop.lab5.common.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import ru.vadimkhalikov.oop.lab5.common.Message;

/**
 * Реализация протокола, использующая стандартную сериализацию Java-объектов.
 */
public class JavaSerializationProtocol implements MessageProtocol {
    
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    
    @Override
    public void sendMessage(Message message, OutputStream out) throws IOException {
        if (objectOut == null) {
            objectOut = new ObjectOutputStream(out);
        }
        
        synchronized (objectOut) {
            objectOut.writeObject(message);
            objectOut.flush();
        }
    }
    
    @Override
    public Message receiveMessage(InputStream in) throws IOException, ClassNotFoundException {
        if (objectIn == null) {
            objectIn = new ObjectInputStream(in);
        }
        
        return (Message) objectIn.readObject();
    }
    
    @Override
    public String getProtocolName() {
        return "java-serialization";
    }
    
    @Override
    public void close() throws IOException {
        if (objectIn != null) {
            objectIn.close();
            objectIn = null;
        }
        
        if (objectOut != null) {
            objectOut.close();
            objectOut = null;
        }
    }
} 