package com.example.websocketdemo.config;


import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.Service.UserLocationService;
import com.example.websocketdemo.manager.ServerInstanceManager;
import com.example.websocketdemo.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.ServerInfo;
import org.springframework.beans.factory.annotation.Autowired;
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

    public void handle(String message) throws Exception {
        //拿到的是消息,拿本地地址，与该message的接收者地址做对比，如果相同，说明在同一个实例，则直接广播即可
        //如果不是的话，则不用处理
        //消息需要自己进行mapper的映射到map类，要保证跟传的时候的数据风格一致，即json风格
        @SuppressWarnings("unchecked")
        Map<String,Object> userMapper=objectMapper.readValue(message,Map.class);
        String receiverName=(String)userMapper.get("receiver");
        String receiverLocation=userLocationService.getUserLocation(receiverName);
        String senderName=(String)userMapper.get("sender");
        if(serverInstanceManager.getInstanceId().equals(receiverName)){
            ChatMessage chatMessage=new ChatMessage();
            chatMessage.setTarget(receiverName);
            chatMessage.setSender(senderName);
            messageSaveService.save(chatMessage);
            if("PRIVATE".equals(chatMessage.getChat_type().toString())){
                template.convertAndSendToUser(chatMessage.getTarget(),"/queue/messages", chatMessage);
                template.convertAndSendToUser(chatMessage.getSender(), "/queue/messages", chatMessage);

            }else if("PUBLIC".equals(chatMessage.getChat_type().toString())){
                //这个时候target实际上是个房间号
                template.convertAndSend("/topic/room/"+chatMessage.getTarget(),chatMessage);
            }
        }
    }
}
