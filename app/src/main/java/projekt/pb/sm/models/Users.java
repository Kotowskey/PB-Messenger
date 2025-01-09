package projekt.pb.sm.models;

public class Users {
    private String userId;
    private String userName;
    private String profilePic;
    private String lastMessage;
    private String status;
    private String lastMessageSenderId;
    private boolean isLastMessageRead;
    private String lastMessageTimestamp;

    public Users() {
    }

    public Users(String userId, String userName, String profilePic) {
        this.userId = userId;
        this.userName = userName;
        this.profilePic = profilePic;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public boolean isLastMessageRead() {
        return isLastMessageRead;
    }

    public void setLastMessageRead(boolean lastMessageRead) {
        this.isLastMessageRead = lastMessageRead;
    }

    public String getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(String lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}