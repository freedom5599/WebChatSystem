package com.chat.controller;

import com.chat.entity.ChatMessage;
import com.chat.entity.GroupMessage;
import com.chat.entity.PrivateMessage;
import com.chat.service.GroupService;
import com.chat.service.PrivateMessageService;
import com.chat.service.UserService;
import com.chat.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class ChatWebSocketController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private PrivateMessageService privateMessageService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private UserService userService;

    private String resolveContent(ChatMessage chatMessage) {
        String content = chatMessage.getContent();
        Integer msgType = chatMessage.getMsgType();
        if (content == null || content.isEmpty()) {
            if (msgType != null) {
                if (msgType == 1) {
                    return "[语音]";
                }
                if (msgType == 2) {
                    return "[图片]";
                }
                if (msgType == 3) {
                    return "[文件]";
                }
            }
            return "";
        }
        return content;
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        PrivateMessage pm = new PrivateMessage();
        pm.setFromId(chatMessage.getFromId());
        pm.setToId(chatMessage.getToId());
        pm.setContent(resolveContent(chatMessage));
        pm.setMsgType(chatMessage.getMsgType() != null ? chatMessage.getMsgType() : 0);
        pm.setVoiceUrl(chatMessage.getVoiceUrl());
        privateMessageService.sendMessage(pm);

        User fromUser = userService.findById(chatMessage.getFromId());
        if (fromUser != null) {
            chatMessage.setFromNickname(fromUser.getNickname());
            chatMessage.setFromAvatar(fromUser.getAvatar());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        chatMessage.setTime(sdf.format(new Date()));

        messagingTemplate.convertAndSend(
                "/topic/private/" + chatMessage.getToId(),
                chatMessage
        );
        messagingTemplate.convertAndSend(
                "/topic/private/" + chatMessage.getFromId(),
                chatMessage
        );
    }

    @MessageMapping("/chat.group")
    public void sendGroupMessage(@Payload ChatMessage chatMessage) {
        GroupMessage gm = new GroupMessage();
        gm.setGroupId(chatMessage.getGroupId());
        gm.setFromId(chatMessage.getFromId());
        gm.setContent(resolveContent(chatMessage));
        gm.setMsgType(chatMessage.getMsgType() != null ? chatMessage.getMsgType() : 0);
        gm.setVoiceUrl(chatMessage.getVoiceUrl());
        groupService.sendGroupMessage(gm);

        User fromUser = userService.findById(chatMessage.getFromId());
        if (fromUser != null) {
            chatMessage.setFromNickname(fromUser.getNickname());
            chatMessage.setFromAvatar(fromUser.getAvatar());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        chatMessage.setTime(sdf.format(new Date()));

        messagingTemplate.convertAndSend("/topic/group/" + chatMessage.getGroupId(), chatMessage);
    }
}
