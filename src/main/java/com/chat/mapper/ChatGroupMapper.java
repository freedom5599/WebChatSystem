package com.chat.mapper;

import com.chat.entity.ChatGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ChatGroupMapper {
    ChatGroup findById(@Param("id") Long id);
    List<ChatGroup> findByUserId(@Param("userId") Long userId);
    int insert(ChatGroup chatGroup);
    int update(ChatGroup chatGroup);
    int deleteById(@Param("id") Long id);
}
