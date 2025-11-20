package com.example.websocketdemo.manager;

import com.example.websocketdemo.handler.MyWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class WebSocketIdleChecker {

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private MyWebSocketHandler handler;

    // 每 5 分钟检查一次
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void checkIdleSessions() {

        long now = System.currentTimeMillis();
        long timeout = 30L * 60 * 1000; // 30分钟

        for (String sessionId : sessionManager.getAll().keySet()) {

            long lastActiveTime = sessionManager.getLastActiveTime(sessionId);

            if (now - lastActiveTime > timeout) {

                WebSocketSession session = handler.getSession(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.close(CloseStatus.GOING_AWAY);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                sessionManager.remove(sessionId);
            }
        }
    }
}

