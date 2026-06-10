package com.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.chat.mapper")
public class ChatSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatSystemApplication.class, args);
    }
}
