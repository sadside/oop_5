package ru.vadimkhalikov.oop.lab5.common.protocol;

/**
 * Перечисление поддерживаемых протоколов обмена сообщениями.
 */
public enum ProtocolType {
    JAVA("java"),
    XML("xml"),
    REST_JSON("rest");
    
    private final String code;
    
    ProtocolType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * Возвращает тип протокола по его строковому коду.
     * 
     * @param code строковой код протокола
     * @return соответствующий тип протокола
     * @throws IllegalArgumentException если код протокола неизвестен
     */
    public static ProtocolType fromString(String code) {
        if (code == null) {
            return JAVA; 
        }
        
        for (ProtocolType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown protocol: " + code);
    }
} 