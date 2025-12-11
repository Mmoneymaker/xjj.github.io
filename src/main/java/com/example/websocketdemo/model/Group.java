package com.example.websocketdemo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊实体类
 */
@Data
@TableName("tb_group")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String groupDesc;

    private String ownerId;

    private String avatarUrl;

    private Integer maxMembers;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer status; // 1-正常，0-解散

    // 群状态枚举
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DISBANDED = 0;
}