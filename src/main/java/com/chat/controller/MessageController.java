package com.chat.controller;

import com.chat.common.Result;
import com.chat.entity.User;
import com.chat.service.PrivateMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/message")
public class MessageController {
    @Autowired
    private PrivateMessageService privateMessageService;

    @GetMapping("/private/history")
    public Result<?> getPrivateHistory(@RequestParam Long friendId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return privateMessageService.getChatHistory(user.getId(), friendId);
    }

    @GetMapping("/private/export")
    public Result<?> exportPrivateHistory(@RequestParam Long friendId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return privateMessageService.exportChatHistory(user.getId(), friendId);
    }

    @GetMapping("/private/history/paged")
    public Result<?> getPrivateHistoryPaged(@RequestParam Long friendId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        int offset = (page - 1) * size;
        return privateMessageService.getChatHistoryPaged(user.getId(), friendId, offset, size);
    }

    @PostMapping("/private/read")
    public Result<?> markAsRead(@RequestParam Long friendId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        privateMessageService.markAsRead(friendId, user.getId());
        return Result.success();
    }
}
