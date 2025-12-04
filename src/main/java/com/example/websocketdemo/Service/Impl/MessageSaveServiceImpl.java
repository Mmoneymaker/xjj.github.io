package com.example.websocketdemo.Service.Impl;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.entity.ChatMessagePO;
import com.example.websocketdemo.exception.BusinessException;
import com.example.websocketdemo.mapper.ChatMessageMapper;
import com.example.websocketdemo.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import  com.example.websocketdemo.exception.BusinessException;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class MessageSaveServiceImpl implements MessageSaveService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    ChatMessageMapper chatMessageMapper;
    @Override
    @Async("taskExecutor")
    public void save(ChatMessage message) {
        long start = System.currentTimeMillis();
        log.info("发送者是{}",message.getSender());
        ChatMessagePO messagePO = new ChatMessagePO();
        messagePO.setSender(message.getSender());
        messagePO.setContent(message.getContent());
        messagePO.setCreate_time(LocalDateTime.now());
        messagePO.setTarget(message.getTarget());
        //ChatType是private还是group
        messagePO.setChatType(message.getChat_type().toString());
        if(chatMessageMapper.insert(messagePO)!=1){
            throw new BusinessException(401,"落库失败");
        }
        log.info("消息已异步落库，耗时: {}ms", System.currentTimeMillis() - start);
    }

    @Override
    public List<ChatMessage> getHistory(String currentUser, String target, String type, int limit) {
        return chatMessageMapper.getChatHistory(currentUser, target, type, limit);
    }

}
