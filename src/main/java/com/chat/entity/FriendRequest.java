package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class FriendRequest {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String message;
    private Integer status;
    private Date createTime;
    private Date handleTime;
    private String fromNickname;
    private String fromAvatar;
    private String fromUsername;
}
