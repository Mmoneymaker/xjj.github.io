package com.example.websocketdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.websocketdemo.model.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 群成员Mapper接口
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /**
     * 查询群聊成员列表
     */
    @Select("SELECT gm.* FROM tb_group_member gm " +
            "LEFT JOIN tb_user u ON gm.username = u.username " +
            "WHERE gm.group_id = #{groupId} AND gm.status = 1 " +
            "ORDER BY gm.role DESC, gm.join_time ASC")
    List<GroupMember> listGroupMembers(@Param("groupId") Long groupId);

    /**
     * 检查用户是否在群聊中
     */
    @Select("SELECT COUNT(*) FROM tb_group_member " +
            "WHERE group_id = #{groupId} AND username = #{username} AND status = 1")
    Integer isMemberInGroup(@Param("groupId") Long groupId, @Param("username") String username);

    /**
     * 更新最后阅读时间
     */
    @Update("UPDATE tb_group_member SET last_read_time = NOW() " +
            "WHERE group_id = #{groupId} AND username = #{username}")
    int updateLastReadTime(@Param("groupId") Long groupId, @Param("username") String username);


    @Select("SELECT COUNT(*) FROM tb_group g INNER JOIN tb_group_member gm ON gm.group_id=#{groupId}")
    Integer getGroupMemberCount(Long groupId);
}