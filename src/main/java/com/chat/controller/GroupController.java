package com.chat.controller;

import com.chat.common.Result;
import com.chat.dto.GroupJoinDTO;
import com.chat.entity.ChatGroup;
import com.chat.entity.User;
import com.chat.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/group")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    public Result<?> createGroup(@RequestBody ChatGroup chatGroup, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        chatGroup.setOwnerId(user.getId());
        return groupService.createGroup(chatGroup);
    }

    @GetMapping("/my")
    public Result<?> getMyGroups(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return groupService.getMyGroups(user.getId());
    }

    @GetMapping("/info/{groupId}")
    public Result<?> getGroupInfo(@PathVariable Long groupId) {
        return groupService.getGroupInfo(groupId);
    }

    @PostMapping("/join")
    public Result<?> joinGroup(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return groupService.joinGroup(dto.getGroupId(), user.getId());
    }

    @PostMapping("/leave")
    public Result<?> leaveGroup(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return groupService.leaveGroup(dto.getGroupId(), user.getId());
    }

    @GetMapping("/members/{groupId}")
    public Result<?> getMembers(@PathVariable Long groupId) {
        return groupService.getMembers(groupId);
    }

    @GetMapping("/messages/{groupId}")
    public Result<?> getGroupMessages(@PathVariable Long groupId) {
        return groupService.getGroupMessages(groupId);
    }

    @GetMapping("/export/{groupId}")
    public Result<?> exportGroupMessages(@PathVariable Long groupId) {
        return groupService.exportGroupMessages(groupId);
    }

    @GetMapping("/messages/paged/{groupId}")
    public Result<?> getGroupMessagesPaged(@PathVariable Long groupId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        return groupService.getGroupMessagesPaged(groupId, offset, size);
    }

    // 邀请好友加入群聊
    @PostMapping("/invite")
    public Result<?> inviteFriend(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return groupService.inviteFriend(dto.getGroupId(), dto.getUserId(), user.getId());
    }

    // 群主踢出群成员
    @PostMapping("/kick")
    public Result<?> kickMember(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return groupService.kickMember(dto.getGroupId(), dto.getUserId(), user.getId());
    }

    // 获取我的群邀请列表
    @GetMapping("/invites")
    public Result<?> getMyInvites(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return groupService.getMyInvites(user.getId());
    }

    // 接受群邀请
    @PostMapping("/invite/accept")
    public Result<?> acceptInvite(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return groupService.acceptInvite(dto.getInviteId(), user.getId());
    }

    // 拒绝群邀请
    @PostMapping("/invite/reject")
    public Result<?> rejectInvite(@RequestBody GroupJoinDTO dto, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return groupService.rejectInvite(dto.getInviteId(), user.getId());
    }
}
