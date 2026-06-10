package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Friend {
    private Long id;
    private Long userId;
    private Long friendId;
    private Long groupId;
    private String remark;
    private Date createTime;
    private String friendNickname;
    private String friendAvatar;
    private String friendSignature;
    private Integer friendStatus;
    private String groupName;
}
