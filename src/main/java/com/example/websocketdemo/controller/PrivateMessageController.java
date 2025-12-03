package com.example.websocketdemo.controller;

import com.example.websocketdemo.model.PrivateMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PrivateMessageController {

    @Autowired
    private SimpMessagingTemplate template;

    //记得做存库处理

    @MessageMapping("/chat.privateMessage")
    public void SendPrivateMessage(@Payload PrivateMessage privateMessage) {
           template.convertAndSendToUser(privateMessage.getSender(),"/queue/messages",privateMessage);

    }


}
