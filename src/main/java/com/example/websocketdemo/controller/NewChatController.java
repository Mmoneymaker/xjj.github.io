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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class NewChatController {

    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    private MessageSaveService messageSaveService;
    @Autowired
    private TokenUtils tokenUtils;
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, @Header(value="Token",required=false) String token
            , SimpMessageHeaderAccessor headerAccessor) {
     //前端不带sender
        String sender = (String) headerAccessor.getSessionAttributes().get("user");
        if (sender != null) {
            chatMessage.setSender(sender);
        }

       //根据消息是私聊还是群聊，将消息转发回去
        //这里我在想是否得做个提醒，比如有人发了消息，但是由于后面出错，没收到，但库里有
        messageSaveService.save(chatMessage);
        if("PRIVATE".equals(chatMessage.getChat_type().toString())){
            template.convertAndSendToUser(chatMessage.getTarget(),"/queue/messages", chatMessage);
            template.convertAndSendToUser(chatMessage.getSender(), "/queue/messages", chatMessage);

        }else if("PUBLIC".equals(chatMessage.getChat_type().toString())){
            //这个时候target实际上是个房间号
            template.convertAndSend("/topic/room/"+chatMessage.getTarget(),chatMessage);
        }

    }



//    @MessageMapping("/chat.addUser")
//    @SendTo("/topic/public")
//    public ChatMessage addUser(@Payload ChatMessage chatMessage,
//                               SimpMessageHeaderAccessor headerAccessor) {
//        // Add username in web socket session
////        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
//        return chatMessage;
//    }

    @MessageMapping("/chat.history")
    public void loadHistory(@Payload Map<String,String> bodyObj,SimpMessageHeaderAccessor headerAccessor) {
        //前端要发target和type(私人还是群聊)
        String currentUser=(String) headerAccessor.getSessionAttributes().get("user");
        String target=bodyObj.get("target");
        String Chattype=bodyObj.get("Chattype");
         // select * from chat_message where "sender"=currentUser,"target"=target,"chat_type"=Chattype
        List<ChatMessage> history = messageSaveService.getHistory(
                currentUser,
                target,
                Chattype
                , 50);

        // 发给自己
        template.convertAndSendToUser(currentUser, "/queue/history", history);

    }

}
