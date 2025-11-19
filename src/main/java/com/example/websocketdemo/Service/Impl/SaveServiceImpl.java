package com.example.websocketdemo.Service.Impl;

import com.example.websocketdemo.Service.MessageSaveService;
import com.example.websocketdemo.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SaveServiceImpl implements MessageSaveService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ChatMessage save(ChatMessage message) {
        Map<String,String> map=new HashMap<>();
        map.put("messageType:",message.getType().toString());
        map.put("Content:",message.getContent());
        map.put("Sender:",message.getSender());
        HashOperations<String, String, String> ops=stringRedisTemplate.opsForHash();
        ops.putAll("WebSocket:5/28",map);
        return message;
    }
}
