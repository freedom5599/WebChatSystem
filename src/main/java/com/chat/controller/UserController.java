package com.chat.controller;

import com.chat.common.Result;
import com.chat.entity.User;
import com.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody User user, HttpSession session) {
        Result<?> result = userService.login(user.getUsername(), user.getPassword());
        if (result.getCode() == 200) {
            User loginUser = (User) result.getData();
            session.setAttribute("loginUser", loginUser);
            messagingTemplate.convertAndSend("/topic/user/status", 
                "{\"userId\":" + loginUser.getId() + ",\"status\":1}");
        }
        return result;
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user != null) {
            userService.logout(user.getId());
            messagingTemplate.convertAndSend("/topic/user/status", 
                "{\"userId\":" + user.getId() + ",\"status\":0}");
            session.invalidate();
        }
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> getInfo(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return userService.getUserById(user.getId());
    }

    @GetMapping("/info/{id}")
    public Result<?> getInfoById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/search")
    public Result<?> searchByUsername(@RequestParam String username, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Result.error(401, "未登录");
        }
        return userService.searchByUsername(username);
    }

    @PostMapping("/update")
    public Result<?> updateProfile(@RequestBody User user, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Result.error(401, "未登录");
        }
        user.setId(loginUser.getId());
        userService.updateProfile(user);
        User updated = userService.findById(loginUser.getId());
        updated.setPassword(null);
        session.setAttribute("loginUser", updated);
        return Result.success(updated);
    }
}
