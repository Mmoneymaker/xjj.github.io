package com.example.websocketdemo.Service;


import com.example.websocketdemo.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MessageSaveService {
    public void save(ChatMessage message);

    public List<ChatMessage> getHistory(String currentUser, String target, String type, int limit) ;
}
