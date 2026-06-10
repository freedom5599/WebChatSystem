package com.chat.entity;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private Long fromId;
    private Long toId;
    private Long groupId;
    private String content;
    private Integer msgType;
    private String voiceUrl;
    private String fromNickname;
    private String fromAvatar;
    private String time;
}
