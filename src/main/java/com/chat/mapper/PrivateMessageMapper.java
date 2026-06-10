package com.chat.mapper;

import com.chat.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PrivateMessageMapper {
    List<PrivateMessage> findChatHistory(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    int insert(PrivateMessage message);
    int markAsRead(@Param("fromId") Long fromId, @Param("toId") Long toId);
    int countUnread(@Param("toId") Long toId, @Param("fromId") Long fromId);
    List<PrivateMessage> findChatHistoryPaged(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("offset") int offset, @Param("limit") int limit);
}
