package com.chat.mapper;

import com.chat.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GroupMemberMapper {
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);
    GroupMember findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    int insert(GroupMember groupMember);
    int delete(@Param("groupId") Long groupId, @Param("userId") Long userId);
    int deleteByGroupId(@Param("groupId") Long groupId);
}
