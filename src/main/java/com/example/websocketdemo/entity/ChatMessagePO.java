package com.example.websocketdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
// ChatMessagePO.java
@TableName("chat_message")
public class ChatMessagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sender;
    private String content;
    private LocalDateTime create_time;
    private String target;        // 一对一：对方用户名；群聊：roomId
    private String chatType;      // PRIVATE 或 GROUP

    // getter & setter 省略
}