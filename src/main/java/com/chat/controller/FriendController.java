package com.chat.controller;

import com.chat.common.Result;
import com.chat.dto.FriendGroupCreateDTO;
import com.chat.dto.FriendHandleDTO;
import com.chat.dto.FriendMoveDTO;
import com.chat.dto.FriendRequestDTO;
import com.chat.entity.User;
import com.chat.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/friend")
public class FriendController {
    @Autowired
    private FriendService friendService;

    @GetMapping("/list")
    public Result<?> getFriendList(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.getFriendList(user.getId());
    }

    @PostMapping("/request")
    public Result<?> sendRequest(@RequestBody FriendRequestDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.sendRequest(user.getId(), dto.getToUserId(), dto.getMessage());
    }

    @PostMapping("/handle")
    public Result<?> handleRequest(@RequestBody FriendHandleDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.handleRequest(dto.getRequestId(), dto.getStatus(), user.getId());
    }

    @GetMapping("/requests")
    public Result<?> getRequests(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.getRequests(user.getId());
    }

    @DeleteMapping("/delete")
    public Result<?> deleteFriend(@RequestParam Long friendId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.deleteFriend(user.getId(), friendId);
    }

    @PostMapping("/move")
    public Result<?> moveFriend(@RequestBody FriendMoveDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.moveFriend(user.getId(), dto.getFriendId(), dto.getGroupId());
    }

    @GetMapping("/groups")
    public Result<?> getGroups(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.getGroups(user.getId());
    }

    @PostMapping("/group/add")
    public Result<?> createGroup(@RequestBody FriendGroupCreateDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.createGroup(user.getId(), dto.getGroupName());
    }

    @DeleteMapping("/group/delete")
    public Result<?> deleteGroup(@RequestParam Long groupId, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return friendService.deleteGroup(groupId, user.getId());
    }
}
