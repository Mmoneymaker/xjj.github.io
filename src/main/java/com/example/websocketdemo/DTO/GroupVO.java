package com.example.websocketdemo.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群聊视图对象
 */
@Data
public class GroupVO {

    private Long id;

    private String groupName;

    private String groupDesc;

    private String ownerId;

    private String avatarUrl;

    private Integer memberCount;

    private LocalDateTime createTime;

    private List<GroupMemberVO> members;

    /**
     * 群成员视图对象
     */
    @Data
    public static class GroupMemberVO {
        private String username;
        private String nickname;
        private Integer role;
        private String roleName; // 角色名称
        private LocalDateTime joinTime;
    }
}