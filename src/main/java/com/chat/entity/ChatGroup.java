package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ChatGroup {
    private Long id;
    private String groupName;
    private String avatar;
    private Long ownerId;
    private String description;
    private Date createTime;
    private String ownerNickname;
    private Integer memberCount;
}
