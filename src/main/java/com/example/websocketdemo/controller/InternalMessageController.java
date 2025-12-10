package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.Service.UserStatusBroadcastService;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.UserStatusEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalMessageController {

    public InternalMessageController() {
        System.out.println("✅ InternalMessageController 已初始化！");
    }
    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    MessageSaveService messageSaveService;
    @Autowired
    private UserStatusBroadcastService userStatusBroadcastService;
    @PostMapping("/message/forward")
    public Boolean ConvertMessageAndSend(@Payload ChatMessage chatMessage) {
        messageSaveService.save(chatMessage);
        if("PRIVATE".equals(chatMessage.getChat_type().toString())){
            template.convertAndSendToUser(chatMessage.getTarget(),"/queue/messages", chatMessage);
            template.convertAndSendToUser(chatMessage.getSender(), "/queue/messages", chatMessage);

        }else if("PUBLIC".equals(chatMessage.getChat_type().toString())){
            //这个时候target实际上是个房间号
            template.convertAndSend("/topic/room/"+chatMessage.getTarget(),chatMessage);
        }
        return Boolean.TRUE;
    }
    @PostMapping("/info")
    public String RefreshUserOnline(@RequestBody(required = false) String username){
        System.out.println("📍 /info 端点被调用，用户名: " + username);

        List<UserStatusEvent> onlineUsers = userStatusBroadcastService.getOnlineUsers();

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ONLINE_LIST");
        message.put("users", onlineUsers);
        message.put("timestamp", System.currentTimeMillis());

        template.convertAndSend("/topic/online-status",message);

        return "True";
    }
}
