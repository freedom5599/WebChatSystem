package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class FriendGroup {
    private Long id;
    private Long userId;
    private String groupName;
    private Integer sortOrder;
    private Date createTime;
}
