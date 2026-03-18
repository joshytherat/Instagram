/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package instagram.Utilities;

/**
 *
 * @author janinadiaz
 */


import java.io.Serializable;

public class NetworkProtocol implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        // Mensajería
        SEND_MESSAGE,
        RECEIVE_MESSAGE,
        MESSAGE_DELIVERED,
        MESSAGE_READ,
        
        // Conversaciones
        GET_CONVERSATIONS,
        CONVERSATIONS_LIST,
        DELETE_CONVERSATION,
        
        // Notificaciones
        NEW_MESSAGE_NOTIFICATION,
        NEW_FOLLOWER_NOTIFICATION,
        NEW_MENTION_NOTIFICATION,
        
        // Autenticación
        LOGIN,
        LOGOUT,
        
        // Estado
        USER_ONLINE,
        USER_OFFLINE,
        
        // Respuestas
        SUCCESS,
        ERROR,
       
    
        // Notificaciones
        NOTIFICATION_NEW,
        NOTIFICATION_GET_ALL,
        NOTIFICATION_GET_UNREAD,
        NOTIFICATION_MARK_READ,
        NOTIFICATION_MARK_ALL_READ,
        NOTIFICATION_DELETE,
        NOTIFICATION_COUNT_UNREAD,

        // Respuestas
        NOTIFICATION_LIST,
        NOTIFICATION_COUNT
    
    }
    
    private MessageType type;
    private String sender;
    private String receiver;
    private Object payload;
    private long timestamp;
    
    public NetworkProtocol(MessageType type, String sender, Object payload) {
        this.type = type;
        this.sender = sender;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    public NetworkProtocol(MessageType type, String sender, String receiver, Object payload) {
        this(type, sender, payload);
        this.receiver = receiver;
    }
    
    // Getters y Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "Protocol{" + type + " from=" + sender + " to=" + receiver + "}";
    }
}