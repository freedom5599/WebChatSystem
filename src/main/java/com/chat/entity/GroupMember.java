package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class GroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private Integer role;
    private Date joinTime;
    private String userNickname;
    private String userAvatar;
    private String userSignature;
    private Integer userStatus;
}
