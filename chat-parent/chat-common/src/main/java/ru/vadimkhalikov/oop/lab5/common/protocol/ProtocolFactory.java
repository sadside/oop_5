package ru.vadimkhalikov.oop.lab5.common.protocol;

/**
 * Фабрика для создания протоколов обмена сообщениями.
 */
public class ProtocolFactory {
    
    /**
     * Строковые константы для обратной совместимости
     * @deprecated Используйте {@link ProtocolType} вместо строковых констант
     */
    @Deprecated
    public static final String PROTOCOL_JAVA = ProtocolType.JAVA.getCode();
    
    /**
     * @deprecated Используйте {@link ProtocolType} вместо строковых констант
     */
    @Deprecated
    public static final String PROTOCOL_XML = ProtocolType.XML.getCode();
    
    /**
     * @deprecated Используйте {@link ProtocolType} вместо строковых констант
     */
    @Deprecated
    public static final String PROTOCOL_REST = ProtocolType.REST_JSON.getCode();
    
    /**
     * Создает протокол по указанному типу.
     * 
     * @param protocolType тип протокола (JAVA, XML или REST_JSON)
     * @return экземпляр соответствующего протокола
     */
    public static MessageProtocol createProtocol(ProtocolType protocolType) {
        switch (protocolType) {
            case JAVA:
                return new JavaSerializationProtocol();
            case XML:
                return new XmlProtocol();
            case REST_JSON:
                return new RestJsonProtocol();
            default:
                throw new IllegalArgumentException("Unsupported protocol type: " + protocolType);
        }
    }
    
    /**
     * Создает протокол по его имени.
     * 
     * @param protocolName имя протокола ("java", "xml" или "rest")
     * @return экземпляр соответствующего протокола
     * @throws IllegalArgumentException если имя протокола неизвестно
     * @deprecated Используйте {@link #createProtocol(ProtocolType)} вместо строковых параметров
     */
    @Deprecated
    public static MessageProtocol createProtocol(String protocolName) {
        return createProtocol(ProtocolType.fromString(protocolName));
    }
} 