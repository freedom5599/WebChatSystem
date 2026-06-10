package com.chat.mapper;

import com.chat.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FriendMapper {
    List<Friend> findFriendsByUserId(@Param("userId") Long userId);
    List<Friend> findFriendsByGroupId(@Param("groupId") Long groupId);
    Friend findByUserIdAndFriendId(@Param("userId") Long userId, @Param("friendId") Long friendId);
    int insert(Friend friend);
    int delete(@Param("userId") Long userId, @Param("friendId") Long friendId);
    int updateGroupId(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("groupId") Long groupId);
    int updateRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);
}
