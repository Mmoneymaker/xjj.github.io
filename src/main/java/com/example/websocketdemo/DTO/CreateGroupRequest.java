package com.example.websocketdemo.DTO;

import lombok.Data;

import java.util.List;

/**
 * 创建群聊请求DTO
 */
@Data
public class CreateGroupRequest {

    private String groupName;

    private String groupDesc;

    private List<String> members; // 初始成员列表（不包括群主）
}