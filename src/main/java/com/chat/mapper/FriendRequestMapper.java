package com.chat.mapper;

import com.chat.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FriendRequestMapper {
    List<FriendRequest> findByToUserId(@Param("toUserId") Long toUserId);
    FriendRequest findById(@Param("id") Long id);
    int insert(FriendRequest friendRequest);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
