package com.example.websocketdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.websocketdemo.DTO.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegistryMapper extends BaseMapper<User>{
}
