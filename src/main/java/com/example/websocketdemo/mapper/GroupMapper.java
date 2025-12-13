package com.example.websocketdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.model.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 群聊Mapper接口
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {

    /**
     * 查询用户所在的群聊列表
     */
    @Select("SELECT g.* FROM tb_group g " +
            "INNER JOIN tb_group_member gm ON g.id = gm.group_id " +
            "WHERE gm.username = #{username} AND gm.status = 1 AND g.status = 1")
    List<Group> listUserGroups(@Param("username") String username);

    /**
     * 查询群成员数量
     */
    @Select("SELECT COUNT(*) FROM tb_group_member WHERE group_id = #{groupId} AND status = 1")
    Integer getGroupMemberCount(@Param("groupId") Long groupId);


    @Select("SELECT * FROM chat_message m where m.group_id=#{groupId}")
    List<ChatMessage> getChatMessages(Long groupId);

    /**
     * 获取群聊历史记录（按时间倒序，限制条数）
     */
    @Select("SELECT * FROM chat_message WHERE group_id = #{groupId} ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatMessage> getGroupHistory(@Param("groupId") Long groupId, @Param("limit") int limit);

    //select * from tb_group INNER JOIN tb_group_member ON tb_group_member.group_id=tb_group.id where tb_group_member.group_id=#{groupId};
}