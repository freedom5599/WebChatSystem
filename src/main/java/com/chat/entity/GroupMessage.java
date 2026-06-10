package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class GroupMessage {
    private Long id;
    private Long groupId;
    private Long fromId;
    private String content;
    private Integer msgType;
    private String voiceUrl;
    private Date createTime;
    private String fromNickname;
    private String fromAvatar;
}
