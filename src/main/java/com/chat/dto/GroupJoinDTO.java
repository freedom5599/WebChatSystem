package com.chat.dto;

import lombok.Data;

@Data
public class GroupJoinDTO {
    private Long groupId;
    private Long userId;
    private Long inviteId;
}
