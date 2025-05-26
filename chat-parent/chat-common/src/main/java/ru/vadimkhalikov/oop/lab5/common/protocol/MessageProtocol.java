package ru.vadimkhalikov.oop.lab5.common.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ru.vadimkhalikov.oop.lab5.common.Message;

/**
 * Интерфейс для протокола обмена сообщениями.
 * Определяет методы для отправки и приема сообщений между клиентом и сервером.
 */
public interface MessageProtocol {
    
    /**
     * Отправляет сообщение в выходной поток
     * 
     * @param message сообщение для отправки
     * @param out выходной поток
     * @throws IOException при ошибке записи в поток
     */
    void sendMessage(Message message, OutputStream out) throws IOException;
    
    /**
     * Читает сообщение из входного потока
     * 
     * @param in входной поток
     * @return прочитанное сообщение
     * @throws IOException при ошибке чтения из потока
     * @throws ClassNotFoundException если класс сообщения не найден
     */
    Message receiveMessage(InputStream in) throws IOException, ClassNotFoundException;
    
    /**
     * Получает имя протокола
     * 
     * @return строковое представление протокола
     */
    String getProtocolName();
    
    /**
     * Закрывает ресурсы протокола, если необходимо
     * 
     * @throws IOException при ошибке закрытия ресурсов
     */
    void close() throws IOException;
} 