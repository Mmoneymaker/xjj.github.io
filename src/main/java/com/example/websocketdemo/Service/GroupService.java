package com.example.websocketdemo.Service;

import com.example.websocketdemo.DTO.CreateGroupRequest;
import com.example.websocketdemo.DTO.GroupVO;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.Group;
import com.example.websocketdemo.model.GroupMember;

import java.util.List;

/**
 * 群聊服务接口
 */
public interface GroupService {

    /**
     * 创建群聊
     */
    GroupVO createGroup(CreateGroupRequest request, String owner);

    /**
     * 加入群聊
     */
    boolean joinGroup(Long groupId, String username);

    /**
     * 退出群聊
     */
    boolean quitGroup(Long groupId, String username);

    /**
     * 获取用户所在的群聊列表
     */
    List<GroupVO> getUserGroups(String username);

    /**
     * 获取群聊详情
     */
    GroupVO getGroupInfo(Long groupId, String username);

    /**
     * 获取群成员列表
     */
    List<GroupVO.GroupMemberVO> getGroupMembers(Long groupId);

    /**
     * 踢出群成员
     */
    boolean removeMember(Long groupId, String owner, String memberUsername);

    /**
     * 检查用户是否在群聊中
     */
    boolean isMemberInGroup(Long groupId, String username);

    /**
     * 更新群聊最后阅读时间
     */
    void updateLastReadTime(Long groupId, String username);

    List<ChatMessage> getChatMessages(Long groupId);

    /**
     * 获取群聊历史记录（带限制条数）
     */
    List<ChatMessage> getGroupHistory(Long groupId, int limit);
}