package projekt.pb.sm.models;

public class Message {
    String messageId, message, senderId, timestamp;
    boolean isRead;  // Dodane nowe pole

    public Message(String messageId, String message, String senderId, String timestamp) {
        this.messageId = messageId;
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.isRead = false;  // Domyślnie wiadomość jest nieodczytana
    }

    public Message() {} // Empty constructor for Firebase

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}