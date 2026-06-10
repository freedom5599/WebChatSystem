package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class GroupInvite {
    private Long id;
    private Long groupId;
    private Long fromUserId;
    private Long toUserId;
    private String message;
    private Integer status;
    private Date createTime;
    private Date handleTime;

    private String groupName;
    private String fromUserNickname;
    private String fromUserAvatar;
}
