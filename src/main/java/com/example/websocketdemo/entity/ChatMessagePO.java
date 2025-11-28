package com.example.websocketdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessagePO {
    private Long id;
    private String sender;
    private String receiver; // 私聊才存，群聊为null
    private String content;
    private String type;
    private LocalDateTime createTime;
}