package com.example.websocketdemo.config;


import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.Service.UserLocationService;
import com.example.websocketdemo.manager.ServerInstanceManager;
import com.example.websocketdemo.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ServerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MessageRouteHandler {
    @Autowired
    private SimpMessageSendingOperations template;

    @Autowired
    UserLocationService userLocationService;

    @Autowired
    ServerInstanceManager serverInstanceManager;

    @Autowired
    MessageSaveService messageSaveService;


    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(String message) {
        //拿到的是消息,拿本地地址，与该message的接收者地址做对比，如果相同，说明在同一个实例，则直接广播即可
        //如果不是的话，则不用处理
        try { //消息需要自己进行mapper的映射到map类，要保证跟传的时候的数据风格一致，即json风格
            ChatMessage msg = objectMapper.readValue(message, ChatMessage.class);
            String receiverName = msg.getTarget();
            String receiverLocation = userLocationService.getUserLocation(receiverName);
            String senderName = msg.getSender();
            String currentInstance = serverInstanceManager.getInstanceId();

            log.info("处理跨实例消息: 发送者={}, 接收者={}, 接收者位置={}, 当前实例={}",
                    senderName, receiverName, receiverLocation, currentInstance);

            messageSaveService.save(msg);

            if (currentInstance.equals(receiverLocation)) {
                log.info("接收者 {} 在当前实例，推送消息", receiverName);
                if ("PRIVATE".equals(msg.getChat_type().toString())) {
                    // 跨实例情况下，只推送给接收者（发送者已经在自己的实例收到消息了）
                    template.convertAndSendToUser(msg.getTarget(), "/queue/messages", msg);
                    log.info("已推送私聊消息给接收者: {}", msg.getTarget());

                } else if ("PUBLIC".equals(msg.getChat_type().toString())) {
                    //这个时候target实际上是个房间号
                    template.convertAndSend("/topic/room/" + msg.getTarget(), msg);
                    log.info("已推送群聊消息到房间: {}", msg.getTarget());
                }
            } else {
                log.info("接收者 {} 不在当前实例，忽略消息", receiverName);
            }
        } catch (Exception e) {
            log.error("处理跨实例消息失败", e);
        }
    }
}
