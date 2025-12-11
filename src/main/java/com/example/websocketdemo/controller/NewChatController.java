package com.example.websocketdemo.controller;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.Service.UserLocationService;
import com.example.websocketdemo.Utils.TokenUtils;
import com.example.websocketdemo.config.RedisPubSubConfig;
import com.example.websocketdemo.manager.ServerInstanceManager;
import com.example.websocketdemo.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ServerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
@Slf4j
@Controller
public class NewChatController {

    @Autowired
    SimpMessagingTemplate template;
    @Autowired
    private MessageSaveService messageSaveService;
    @Autowired
    private TokenUtils tokenUtils;
    @Autowired
    private ServerInstanceManager serverInstanceManager;
    @Autowired
    private UserLocationService userLocationService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage, @Header(value="Token",required=false) String token
            , SimpMessageHeaderAccessor headerAccessor) {
     //前端不带sender
        String sender = (String) headerAccessor.getSessionAttributes().get("user");
        if (sender != null) {
            chatMessage.setSender(sender);
        }

     String senderAddress= serverInstanceManager.getInstanceId();

     // 根据消息类型处理
     if("PRIVATE".equals(chatMessage.getChat_type().toString())){
         // 私聊消息
         String receiverAddress=userLocationService.getUserLocation(chatMessage.getTarget());
         if(senderAddress.equals(receiverAddress)){
             //本地私聊
             messageSaveService.save(chatMessage);
             template.convertAndSendToUser(chatMessage.getTarget(),"/queue/messages", chatMessage);
             template.convertAndSendToUser(chatMessage.getSender(), "/queue/messages", chatMessage);
         } else {
             //跨实例私聊
             messageSaveService.save(chatMessage);
             // 先本地推送给发送者
             template.convertAndSendToUser(chatMessage.getSender(), "/queue/messages", chatMessage);
             // 通过Redis发送给接收者实例
             try {
                 String jsonMessage = objectMapper.writeValueAsString(chatMessage);
                 redisTemplate.convertAndSend(RedisPubSubConfig.MESSAGE_CHANNEL, jsonMessage);
                 log.info("通过Redis发送跨实例私聊消息: {}", jsonMessage);
             } catch (Exception e) {
                 log.error("序列化消息失败", e);
                 // 降级到HTTP
                 restTemplate.postForObject("http://"+receiverAddress+"/internal/message/forward", chatMessage, Boolean.class);
             }
         }
     } else if("GROUP".equals(chatMessage.getChat_type().toString())){
         // 群聊消息
         // target是群ID
         Long groupId = Long.parseLong(chatMessage.getTarget());
         chatMessage.setGroupId(groupId);

         // 保存群聊消息
         messageSaveService.save(chatMessage);

         // 推送到群聊topic（所有实例的群成员都能收到）
         template.convertAndSend("/topic/group/" + groupId, chatMessage);

         // 同时通过Redis通知其他实例有新群消息
         try {
             String jsonMessage = objectMapper.writeValueAsString(chatMessage);
             redisTemplate.convertAndSend(RedisPubSubConfig.MESSAGE_CHANNEL, jsonMessage);
             log.info("通过Redis发送群聊消息: {}", jsonMessage);
         } catch (Exception e) {
             log.error("序列化群聊消息失败", e);
         }
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
