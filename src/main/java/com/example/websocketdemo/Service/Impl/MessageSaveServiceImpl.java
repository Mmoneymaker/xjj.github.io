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
        messagePO.setType(message.getType().name());
        messagePO.setContent(message.getContent());
        messagePO.setCreateTime(LocalDateTime.now());

        if(chatMessageMapper.insert(messagePO)!=1){
            throw new BusinessException(401,"落库失败");
        }
        log.info("消息已异步落库，耗时: {}ms", System.currentTimeMillis() - start);
    }
}
