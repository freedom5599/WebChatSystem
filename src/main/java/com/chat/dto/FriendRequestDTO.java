package com.chat.dto;

import lombok.Data;

@Data
public class FriendRequestDTO {
    private Long toUserId;
    private String message;
}
