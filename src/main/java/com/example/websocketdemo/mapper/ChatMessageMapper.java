package com.example.websocketdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.websocketdemo.entity.ChatMessagePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper <ChatMessagePO>{
}
