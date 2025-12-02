package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.Utils.TokenUtils;
import com.example.websocketdemo.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Created by rajeevkumarsingh on 24/07/17.
 */
@Controller
public class ChatController {

    @Autowired
    private MessageSaveService messageSaveService;
    @Autowired
    private TokenUtils tokenUtils;
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, @Header(value="Token",required=false) String token
    ,SimpMessageHeaderAccessor headerAccessor) {
        //要判断前端传来的sender是否被修改了
        Map<String,Object> mes =headerAccessor.getSessionAttributes();
        String sender=chatMessage.getSender();
        if(mes.get(sender)!=null){
            //不管传来的sender是假的，但是token里存的用户是真的，直接去解析token，强制设置sender为解析的结果
            String realSender=tokenUtils.getUsernameFromJwtToken(token);
            chatMessage.setSender(realSender);
        }else{
            if (token != null) {
                String usernameFromToken = tokenUtils.getUsernameFromJwtToken(token);
                chatMessage.setSender(usernameFromToken);
            } else {
                throw new IllegalArgumentException("无法确定发送者身份");
            }
        }
        messageSaveService.save(chatMessage);
        return chatMessage;
    }



    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
//        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }

}
