package com.example.websocketdemo.Service;


import com.example.websocketdemo.model.ChatMessage;
import org.springframework.stereotype.Service;

@Service
public interface MessageSaveService {
    public ChatMessage save(ChatMessage message);
}
