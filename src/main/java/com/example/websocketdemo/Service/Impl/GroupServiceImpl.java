package com.example.websocketdemo.Service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.websocketdemo.DTO.CreateGroupRequest;
import com.example.websocketdemo.DTO.GroupVO;
import com.example.websocketdemo.Service.GroupService;
import com.example.websocketdemo.mapper.GroupMapper;
import com.example.websocketdemo.mapper.GroupMemberMapper;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.Group;
import com.example.websocketdemo.model.GroupMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    @Transactional
    public GroupVO createGroup(CreateGroupRequest request, String owner) {
        // 创建群聊
        Group group = new Group();
        group.setGroupName(request.getGroupName());
        group.setGroupDesc(request.getGroupDesc());
        group.setOwnerId(owner);
        group.setMaxMembers(200);
        group.setStatus(Group.STATUS_ACTIVE);
        group.setCreateTime(LocalDateTime.now());
        groupMapper.insert(group);

        // 添加群主为成员
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUsername(owner);
        ownerMember.setRole(GroupMember.ROLE_OWNER);
        ownerMember.setJoinTime(LocalDateTime.now());
        ownerMember.setStatus(GroupMember.STATUS_ACTIVE);
        groupMemberMapper.insert(ownerMember);

        // 添加初始成员
        if (request.getMembers() != null && !request.getMembers().isEmpty()) {
            for (String member : request.getMembers()) {
                // 检查是否是群主（群主已经添加过了）
                if (!member.equals(owner)) {
                    // 检查是否已经在群中
                    Integer existing = groupMemberMapper.isMemberInGroup(group.getId(), member);
                    if (existing == null || existing == 0) {
                        GroupMember gm = new GroupMember();
                        gm.setGroupId(group.getId());
                        gm.setUsername(member);
                        gm.setRole(GroupMember.ROLE_MEMBER);
                        gm.setJoinTime(LocalDateTime.now());
                        gm.setStatus(GroupMember.STATUS_ACTIVE);
                        groupMemberMapper.insert(gm);
                    }
                }
            }
        }

        log.info("创建群聊成功: 群ID={}, 群名={}, 群主={}", group.getId(), group.getGroupName(), owner);

        return convertToGroupVO(group);
    }

    @Override
    @Transactional
    public boolean joinGroup(Long groupId, String username) {
        // 检查是否已在群中
        Integer count = groupMemberMapper.isMemberInGroup(groupId, username);
        if (count > 0) {
            return false;
        }

        // 检查群聊是否存在且正常
        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != Group.STATUS_ACTIVE) {
            return false;
        }

        // 检查成员数量是否已达上限
        Integer memberCount = groupMemberMapper.getGroupMemberCount(groupId);
        if (memberCount >= group.getMaxMembers()) {
            return false;
        }

        // 添加成员
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUsername(username);
        member.setRole(GroupMember.ROLE_MEMBER);
        member.setJoinTime(LocalDateTime.now());
        member.setStatus(GroupMember.STATUS_ACTIVE);
        int result = groupMemberMapper.insert(member);

        log.info("用户 {} 加入群聊 {}", username, groupId);
        return result > 0;
    }

    @Override
    @Transactional
    public boolean quitGroup(Long groupId, String username) {
        // 检查是否在群中
        GroupMember member = groupMemberMapper.selectOne(
            new QueryWrapper<GroupMember>()
                .eq("group_id", groupId)
                .eq("username", username)
                .eq("status", GroupMember.STATUS_ACTIVE)
        );

        if (member == null) {
            return false;
        }

        // 群主不能直接退群，需要先转让群主
        if (member.getRole() == GroupMember.ROLE_OWNER) {
            return false;
        }

        // 退出群聊（软删除）
        UpdateWrapper<GroupMember> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("group_id", groupId)
                    .eq("username", username)
                    .set("status", GroupMember.STATUS_QUIT);
        int result = groupMemberMapper.update(null, updateWrapper);

        log.info("用户 {} 退出群聊 {}", username, groupId);
        return result > 0;
    }

    @Override
    public List<GroupVO> getUserGroups(String username) {
        log.info("获取用户 {} 的群聊列表", username);
        List<Group> groups = groupMapper.listUserGroups(username);
        log.info("查询到 {} 个群聊", groups.size());

        return groups.stream()
                .map(group -> {
                    log.debug("群聊 {}: owner={}, 状态={}", group.getId(), group.getOwnerId(), group.getStatus());
                    GroupVO vo = convertToGroupVO(group);
                    // 获取成员数量
                    Integer memberCount = groupMemberMapper.getGroupMemberCount(group.getId());
                    vo.setMemberCount(memberCount);
                    log.debug("群聊 {} 成员数: {}", group.getId(), memberCount);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public GroupVO getGroupInfo(Long groupId, String username) {
        // 检查用户是否在群中
        if (!isMemberInGroup(groupId, username)) {
            return null;
        }

        Group group = groupMapper.selectById(groupId);
        if (group == null || group.getStatus() != Group.STATUS_ACTIVE) {
            return null;
        }

        GroupVO vo = convertToGroupVO(group);
        Integer memberCount = groupMemberMapper.getGroupMemberCount(groupId);
        vo.setMemberCount(memberCount);

        return vo;
    }

    @Override
    public List<GroupVO.GroupMemberVO> getGroupMembers(Long groupId) {
        List<GroupMember> members = groupMemberMapper.listGroupMembers(groupId);
        return members.stream()
                .map(this::convertToGroupMemberVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean removeMember(Long groupId, String owner, String memberUsername) {
        // 检查操作者是否是群主
        GroupMember operator = groupMemberMapper.selectOne(
            new QueryWrapper<GroupMember>()
                .eq("group_id", groupId)
                .eq("username", owner)
                .eq("status", GroupMember.STATUS_ACTIVE)
        );

        if (operator == null || operator.getRole() != GroupMember.ROLE_OWNER) {
            return false;
        }

        // 不能踢出群主
        GroupMember target = groupMemberMapper.selectOne(
            new QueryWrapper<GroupMember>()
                .eq("group_id", groupId)
                .eq("username", memberUsername)
                .eq("status", GroupMember.STATUS_ACTIVE)
        );

        if (target == null || target.getRole() == GroupMember.ROLE_OWNER) {
            return false;
        }

        // 踢出成员
        UpdateWrapper<GroupMember> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("group_id", groupId)
                    .eq("username", memberUsername)
                    .set("status", GroupMember.STATUS_QUIT);
        int result = groupMemberMapper.update(null, updateWrapper);

        log.info("群主 {} 将成员 {} 踢出群聊 {}", owner, memberUsername, groupId);
        return result > 0;
    }

    @Override
    public boolean isMemberInGroup(Long groupId, String username) {
        Integer count = groupMemberMapper.isMemberInGroup(groupId, username);
        return count > 0;
    }

    @Override
    public void updateLastReadTime(Long groupId, String username) {
        groupMemberMapper.updateLastReadTime(groupId, username);
    }

    /**
     * 从群聊中读取历史记录，只需要个groupId即可
     */
    @Override
    public List<ChatMessage> getChatMessages(Long groupId) {
        return groupMapper.getChatMessages(groupId);
    }

    /**
     * 获取群聊历史记录（带限制条数）
     */
    @Override
    public List<ChatMessage> getGroupHistory(Long groupId, int limit) {
        return groupMapper.getGroupHistory(groupId, limit);
    }



    /**
     * 转换Group到GroupVO
     */
    private GroupVO convertToGroupVO(Group group) {
        GroupVO vo = new GroupVO();
        //********这vo的成员好像没有设置吧
        BeanUtils.copyProperties(group, vo);
        return vo;
    }

    /**
     * 转换GroupMember到GroupMemberVO
     */
    private GroupVO.GroupMemberVO convertToGroupMemberVO(GroupMember member) {
        GroupVO.GroupMemberVO vo = new GroupVO.GroupMemberVO();
        vo.setUsername(member.getUsername());
        vo.setNickname(member.getNickname());
        vo.setRole(member.getRole());
        vo.setJoinTime(member.getJoinTime());

        // 角色名称
        switch (member.getRole()) {
            case GroupMember.ROLE_OWNER:
                vo.setRoleName("群主");
                break;
            case GroupMember.ROLE_ADMIN:
                vo.setRoleName("管理员");
                break;
            default:
                vo.setRoleName("成员");
                break;
        }

        return vo;
    }


}