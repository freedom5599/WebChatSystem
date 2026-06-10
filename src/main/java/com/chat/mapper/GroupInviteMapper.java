package com.chat.mapper;

import com.chat.entity.GroupInvite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GroupInviteMapper {
    List<GroupInvite> findByToUserId(@Param("toUserId") Long toUserId);
    GroupInvite findById(@Param("id") Long id);
    int insert(GroupInvite groupInvite);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int deleteById(@Param("id") Long id);
}
