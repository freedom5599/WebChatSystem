package com.chat.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
