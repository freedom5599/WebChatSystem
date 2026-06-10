package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class PrivateMessage {
    private Long id;
    private Long fromId;
    private Long toId;
    private String content;
    private Integer msgType;
    private String voiceUrl;
    private Integer isRead;
    private Date createTime;
    private String fromNickname;
    private String fromAvatar;
}
