package com.example.websocketdemo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// UserStatusEvent.java - 状态事件对象
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusEvent {
    public enum StatusType {
        ONLINE,     // 上线
        OFFLINE,    // 下线
        HEARTBEAT   // 心跳（可选）
    }

    private String username;    // 可展示的名称
    private StatusType status;
    private Long logintime;
    private String avatar;      // 头像URL（可选）

    public UserStatusEvent(String username, StatusType status) {
        this.username = username;
        this.status = status;
        this.logintime = System.currentTimeMillis();
    }
}
