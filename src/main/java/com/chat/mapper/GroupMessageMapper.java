package com.chat.mapper;

import com.chat.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface GroupMessageMapper {
    List<GroupMessage> findByGroupId(@Param("groupId") Long groupId);
    List<GroupMessage> findByGroupIdPaged(@Param("groupId") Long groupId, @Param("offset") int offset, @Param("limit") int limit);
    int insert(GroupMessage message);
}
