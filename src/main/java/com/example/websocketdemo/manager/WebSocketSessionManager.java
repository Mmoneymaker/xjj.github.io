package com.example.websocketdemo.manager;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    // 保存 sessionId -> 最后活动时间戳  //必须用高并发的concurrenthashmap
    private final Map<String, Long> lastActiveTimeMap = new ConcurrentHashMap<>();

    public void refresh(String sessionId) {
        lastActiveTimeMap.put(sessionId, System.currentTimeMillis());
    }

    public Long getLastActiveTime(String sessionId) {
        return lastActiveTimeMap.get(sessionId);
    }

    public void remove(String sessionId) {
        lastActiveTimeMap.remove(sessionId);
    }

    public Map<String, Long> getAll() {
        return lastActiveTimeMap;
    }
}

