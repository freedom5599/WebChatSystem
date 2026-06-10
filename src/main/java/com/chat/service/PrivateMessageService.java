package com.chat.service;

import com.chat.common.Result;
import com.chat.entity.PrivateMessage;
import com.chat.mapper.PrivateMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PrivateMessageService {
    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    public Result<?> getChatHistory(Long userId1, Long userId2) {
        List<PrivateMessage> messages = privateMessageMapper.findChatHistory(userId1, userId2);
        privateMessageMapper.markAsRead(userId2, userId1);
        return Result.success(messages);
    }

    public Result<?> sendMessage(PrivateMessage message) {
        privateMessageMapper.insert(message);
        return Result.success(message);
    }

    public Result<?> exportChatHistory(Long userId1, Long userId2) {
        List<PrivateMessage> messages = privateMessageMapper.findChatHistory(userId1, userId2);
        StringBuilder sb = new StringBuilder();
        sb.append("聊天记录导出\r\n");
        sb.append("================================\r\n\r\n");
        for (PrivateMessage msg : messages) {
            String sender = msg.getFromNickname() != null ? msg.getFromNickname() : "用户" + msg.getFromId();
            String time = msg.getCreateTime() != null ? msg.getCreateTime().toString() : "";
            sb.append("[").append(time).append("] ").append(sender).append(":\r\n");
            sb.append(msg.getContent()).append("\r\n\r\n");
        }
        return Result.success(sb.toString());
    }

    public Result<?> getChatHistoryPaged(Long userId1, Long userId2, int offset, int limit) {
        List<PrivateMessage> messages = privateMessageMapper.findChatHistoryPaged(userId1, userId2, offset, limit);
        java.util.Collections.reverse(messages);
        return Result.success(messages);
    }

    public void markAsRead(Long fromId, Long toId) {
        privateMessageMapper.markAsRead(fromId, toId);
    }
}
