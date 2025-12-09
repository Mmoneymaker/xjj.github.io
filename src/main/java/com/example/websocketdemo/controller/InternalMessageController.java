package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("internal")
public class InternalMessageController {
    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    MessageSaveService messageSaveService;
    @PostMapping("message/forward")
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
}
