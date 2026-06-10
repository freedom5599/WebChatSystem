package com.chat.common;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final ConcurrentHashMap<Long, String> onlineUsers = new ConcurrentHashMap<>();

    public void addOnlineUser(Long userId, String sessionId) {
        onlineUsers.put(userId, sessionId);
    }

    public void removeOnlineUser(Long userId) {
        onlineUsers.remove(userId);
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    public ConcurrentHashMap<Long, String> getOnlineUsers() {
        return onlineUsers;
    }
}
