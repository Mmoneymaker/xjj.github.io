package com.example.websocketdemo.controller;

import com.example.websocketdemo.DTO.CreateGroupRequest;
import com.example.websocketdemo.DTO.GroupVO;
import com.example.websocketdemo.Service.GroupService;
import com.example.websocketdemo.Utils.TokenUtils;
import com.example.websocketdemo.common.Result;
import com.example.websocketdemo.model.ChatMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * 群聊控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        // 1. 先尝试从 Principal 获取（如果有安全上下文）
//        if (request.getUserPrincipal() != null) {
//            return request.getUserPrincipal().getName();
//        }

        // 2. 从 Token 获取
        return TokenUtils.getUsernameFromToken(request);
    }

    /**
     * 创建群聊
     */
    @PostMapping("/create")
    public Result<GroupVO> createGroup(@RequestBody CreateGroupRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            // 参数校验
            if (request.getGroupName() == null || request.getGroupName().trim().isEmpty()) {
                return Result.error(400, "群聊名称不能为空");
            }

            GroupVO groupVO = groupService.createGroup(request, username);
            return Result.success(groupVO);
        } catch (Exception e) {
            log.error("创建群聊失败", e);
            return Result.error(500, "创建群聊失败");
        }
    }

    /**
     * 加入群聊
     */
    @PostMapping("/{groupId}/join")
    public Result<String> joinGroup(@PathVariable Long groupId,
                                    HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = groupService.joinGroup(groupId, username);
            if (success) {
                return Result.success("加入群聊成功");
            } else {
                return Result.error(400, "加入群聊失败，群聊不存在或已满员");
            }
        } catch (Exception e) {
            log.error("加入群聊失败", e);
            return Result.error(500, "加入群聊失败");
        }
    }

    /**
     * 退出群聊
     */
    @PostMapping("/{groupId}/quit")
    public Result<String> quitGroup(@PathVariable Long groupId,
                                    HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = groupService.quitGroup(groupId, username);
            if (success) {
                return Result.success("退出群聊成功");
            } else {
                return Result.error(400, "退出群聊失败，您可能不在该群聊中或是群主");
            }
        } catch (Exception e) {
            log.error("退出群聊失败", e);
            return Result.error(500, "退出群聊失败");
        }
    }

    /**
     * 获取用户的群聊列表
     */
    @GetMapping("/list")
    public Result<List<GroupVO>> getUserGroups(HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            List<GroupVO> groups = groupService.getUserGroups(username);
            return Result.success(groups);
        } catch (Exception e) {
            log.error("获取群聊列表失败", e);
            return Result.error(500, "获取群聊列表失败");
        }
    }

    /**
     * 获取群聊详情
     */
    @GetMapping("/{groupId}")
    public Result<GroupVO> getGroupInfo(@PathVariable Long groupId,
                                       HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            GroupVO groupVO = groupService.getGroupInfo(groupId, username);
            if (groupVO != null) {
                return Result.success(groupVO);
            } else {
                return Result.error(404, "群聊不存在或您不在该群聊中");
            }
        } catch (Exception e) {
            log.error("获取群聊详情失败", e);
            return Result.error(500, "获取群聊详情失败");
        }
    }

    /**
     * 获取群成员列表
     */
    @GetMapping("/{groupId}/members")
    public Result<List<GroupVO.GroupMemberVO>> getGroupMembers(@PathVariable Long groupId,
                                                               HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            // 检查用户是否在群中
            if (!groupService.isMemberInGroup(groupId, username)) {
                return Result.error(403, "您不在该群聊中");
            }

            List<GroupVO.GroupMemberVO> members = groupService.getGroupMembers(groupId);
            return Result.success(members);
        } catch (Exception e) {
            log.error("获取群成员列表失败", e);
            return Result.error(500, "获取群成员列表失败");
        }
    }

    /**
     * 踢出群成员
     */
    @PostMapping("/{groupId}/remove/{memberUsername}")
    public Result<String> removeMember(@PathVariable Long groupId,
                                       @PathVariable String memberUsername,
                                       HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            boolean success = groupService.removeMember(groupId, username, memberUsername);
            if (success) {
                return Result.success("踢出成员成功");
            } else {
                return Result.error(400, "踢出成员失败，权限不足或成员不存在");
            }
        } catch (Exception e) {
            log.error("踢出群成员失败", e);
            return Result.error(500, "踢出群成员失败");
        }
    }

    /**
     * 更新群聊最后阅读时间
     */
    @PostMapping("/{groupId}/read")
    public Result<String> updateLastReadTime(@PathVariable Long groupId,
                                            HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            groupService.updateLastReadTime(groupId, username);
            return Result.success("更新阅读时间成功");
        } catch (Exception e) {
            log.error("更新阅读时间失败", e);
            return Result.error(500, "更新阅读时间失败");
        }
    }

    /**
     * 获取群聊历史记录
     */
    @PostMapping("/{groupId}/history")
    public Result<List<ChatMessage>> loadHistory(@PathVariable Long groupId,
                                                @RequestParam(defaultValue = "50") int limit,
                                                HttpServletRequest httpRequest) {
        try {
            String username = getCurrentUsername(httpRequest);
            if (username == null) {
                return Result.error(401, "用户未登录");
            }

            // 检查用户是否在群中
            if (!groupService.isMemberInGroup(groupId, username)) {
                return Result.error(403, "您不在该群聊中");
            }

            // 获取群聊历史记录
            List<ChatMessage> history = groupService.getGroupHistory(groupId, limit);

            // 更新最后阅读时间
            groupService.updateLastReadTime(groupId, username);

            return Result.success(history);
        } catch (Exception e) {
            log.error("获取群聊历史记录失败", e);
            return Result.error(500, "获取群聊历史记录失败");
        }
    }
}