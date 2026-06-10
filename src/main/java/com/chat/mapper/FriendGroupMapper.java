package com.chat.mapper;

import com.chat.entity.FriendGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FriendGroupMapper {
    List<FriendGroup> findByUserId(@Param("userId") Long userId);
    FriendGroup findById(@Param("id") Long id);
    int insert(FriendGroup friendGroup);
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);
    int updateName(@Param("id") Long id, @Param("userId") Long userId, @Param("groupName") String groupName);
}
