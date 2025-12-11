package com.example.websocketdemo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员实体类
 */
@Data
@TableName("tb_group_member")
public class GroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private String username;

    private String nickname;

    private Integer role; // 0-普通成员，1-管理员，2-群主

    private LocalDateTime joinTime;

    private LocalDateTime muteUntil;

    private LocalDateTime lastReadTime;

    private Integer status; // 1-正常，0-已退出

    // 角色枚举
    public static final int ROLE_MEMBER = 0;
    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_OWNER = 2;

    // 状态枚举
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_QUIT = 0;
}