package com.example.websocketdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.websocketdemo.entity.ChatMessagePO;
import com.example.websocketdemo.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper <ChatMessagePO>{
    List<ChatMessage> getChatHistory(String currentUser, String target, String type, int limit);
}
