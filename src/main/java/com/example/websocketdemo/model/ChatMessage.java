package com.example.websocketdemo.model;

/**
 * Created by rajeevkumarsingh on 24/07/17.
 */
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private ChatType chat_type;
    private String target;
    private Long groupId; // 群聊ID，群聊消息时使用

    public enum ChatType {
        PRIVATE,
        GROUP
    }
    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public ChatType getChat_type() {
        return chat_type;
    }

    public void setChat_type(ChatType chat_type) {
        this.chat_type = chat_type;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
