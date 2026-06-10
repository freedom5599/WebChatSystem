package com.chat.service;

import com.chat.common.Result;
import com.chat.entity.ChatGroup;
import com.chat.entity.GroupInvite;
import com.chat.entity.GroupMember;
import com.chat.entity.GroupMessage;
import com.chat.mapper.ChatGroupMapper;
import com.chat.mapper.GroupInviteMapper;
import com.chat.mapper.GroupMemberMapper;
import com.chat.mapper.GroupMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupService {
    @Autowired
    private ChatGroupMapper chatGroupMapper;
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    @Autowired
    private GroupMessageMapper groupMessageMapper;
    @Autowired
    private GroupInviteMapper groupInviteMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Result<?> createGroup(ChatGroup chatGroup) {
        chatGroupMapper.insert(chatGroup);
        GroupMember member = new GroupMember();
        member.setGroupId(chatGroup.getId());
        member.setUserId(chatGroup.getOwnerId());
        member.setRole(2);
        groupMemberMapper.insert(member);
        return Result.success(chatGroup);
    }

    public Result<?> getMyGroups(Long userId) {
        List<ChatGroup> groups = chatGroupMapper.findByUserId(userId);
        return Result.success(groups);
    }

    public Result<?> getGroupInfo(Long groupId) {
        ChatGroup group = chatGroupMapper.findById(groupId);
        if (group == null) {
            return Result.error("群组不存在");
        }
        return Result.success(group);
    }

    public Result<?> joinGroup(Long groupId, Long userId) {
        ChatGroup group = chatGroupMapper.findById(groupId);
        if (group == null) {
            return Result.error("群组不存在");
        }
        GroupMember exist = groupMemberMapper.findByGroupIdAndUserId(groupId, userId);
        if (exist != null) {
            return Result.error("已在群中");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(0);
        groupMemberMapper.insert(member);
        return Result.success();
    }

    @Transactional
    public Result<?> leaveGroup(Long groupId, Long userId) {
        ChatGroup group = chatGroupMapper.findById(groupId);
        if (group != null && group.getOwnerId().equals(userId)) {
            groupMemberMapper.deleteByGroupId(groupId);
            chatGroupMapper.deleteById(groupId);
            return Result.success("群已解散");
        }
        groupMemberMapper.delete(groupId, userId);
        return Result.success("已退出群聊");
    }

    public Result<?> getMembers(Long groupId) {
        List<GroupMember> members = groupMemberMapper.findByGroupId(groupId);
        return Result.success(members);
    }

    public Result<?> getGroupMessages(Long groupId) {
        List<GroupMessage> messages = groupMessageMapper.findByGroupId(groupId);
        return Result.success(messages);
    }

    public Result<?> sendGroupMessage(GroupMessage message) {
        groupMessageMapper.insert(message);
        return Result.success(message);
    }

    public Result<?> exportGroupMessages(Long groupId) {
        List<GroupMessage> messages = groupMessageMapper.findByGroupId(groupId);
        ChatGroup group = chatGroupMapper.findById(groupId);
        StringBuilder sb = new StringBuilder();
        sb.append("群聊记录导出 - ").append(group != null ? group.getGroupName() : "").append("\r\n");
        sb.append("================================\r\n\r\n");
        for (GroupMessage msg : messages) {
            String sender = msg.getFromNickname() != null ? msg.getFromNickname() : "用户" + msg.getFromId();
            String time = msg.getCreateTime() != null ? msg.getCreateTime().toString() : "";
            sb.append("[").append(time).append("] ").append(sender).append(":\r\n");
            sb.append(msg.getContent()).append("\r\n\r\n");
        }
        return Result.success(sb.toString());
    }

    public Result<?> getGroupMessagesPaged(Long groupId, int offset, int limit) {
        List<GroupMessage> messages = groupMessageMapper.findByGroupIdPaged(groupId, offset, limit);
        java.util.Collections.reverse(messages);
        return Result.success(messages);
    }

    // 邀请好友加入群聊 - 发送邀请需对方同意
    public Result<?> inviteFriend(Long groupId, Long inviteUserId, Long operatorId) {
        ChatGroup group = chatGroupMapper.findById(groupId);
        if (group == null) {
            return Result.error("群组不存在");
        }
        GroupMember operator = groupMemberMapper.findByGroupIdAndUserId(groupId, operatorId);
        if (operator == null) {
            return Result.error("你不在该群中，无法邀请");
        }
        GroupMember exist = groupMemberMapper.findByGroupIdAndUserId(groupId, inviteUserId);
        if (exist != null) {
            return Result.error("该用户已在群中");
        }

        GroupInvite invite = new GroupInvite();
        invite.setGroupId(groupId);
        invite.setFromUserId(operatorId);
        invite.setToUserId(inviteUserId);
        invite.setMessage("邀请你加入群聊【" + group.getGroupName() + "】");
        invite.setStatus(0);
        groupInviteMapper.insert(invite);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "group_invite");
        payload.put("inviteId", invite.getId());
        payload.put("groupId", groupId);
        payload.put("groupName", group.getGroupName());
        payload.put("fromUserId", operatorId);
        messagingTemplate.convertAndSend("/topic/notification/" + inviteUserId, payload);

        return Result.success("邀请已发送，等待对方同意");
    }

    // 群主踢出群成员
    @Transactional
    public Result<?> kickMember(Long groupId, Long kickUserId, Long operatorId) {
        ChatGroup group = chatGroupMapper.findById(groupId);
        if (group == null) {
            return Result.error("群组不存在");
        }
        if (!group.getOwnerId().equals(operatorId)) {
            return Result.error("只有群主才能踢人");
        }
        if (kickUserId.equals(operatorId)) {
            return Result.error("不能踢出自己");
        }
        GroupMember exist = groupMemberMapper.findByGroupIdAndUserId(groupId, kickUserId);
        if (exist == null) {
            return Result.error("该用户不在群中");
        }
        groupMemberMapper.delete(groupId, kickUserId);
        return Result.success("已将该成员移出群聊");
    }

    // 获取我的群邀请列表
    public Result<?> getMyInvites(Long userId) {
        List<GroupInvite> invites = groupInviteMapper.findByToUserId(userId);
        return Result.success(invites);
    }

    // 接受群邀请
    @Transactional
    public Result<?> acceptInvite(Long inviteId, Long currentUserId) {
        GroupInvite invite = groupInviteMapper.findById(inviteId);
        if (invite == null) {
            return Result.error("邀请不存在");
        }
        if (!invite.getToUserId().equals(currentUserId)) {
            return Result.error("无权处理该邀请");
        }
        if (invite.getStatus() != 0) {
            return Result.error("该邀请已处理");
        }
        GroupMember exist = groupMemberMapper.findByGroupIdAndUserId(invite.getGroupId(), currentUserId);
        if (exist != null) {
            groupInviteMapper.deleteById(inviteId);
            return Result.success("已在群中");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(invite.getGroupId());
        member.setUserId(currentUserId);
        member.setRole(0);
        groupMemberMapper.insert(member);
        groupInviteMapper.deleteById(inviteId);
        return Result.success();
    }

    // 拒绝群邀请
    public Result<?> rejectInvite(Long inviteId, Long currentUserId) {
        GroupInvite invite = groupInviteMapper.findById(inviteId);
        if (invite == null) {
            return Result.error("邀请不存在");
        }
        if (!invite.getToUserId().equals(currentUserId)) {
            return Result.error("无权处理该邀请");
        }
        if (invite.getStatus() != 0) {
            return Result.error("该邀请已处理");
        }
        groupInviteMapper.deleteById(inviteId);
        return Result.success();
    }
}
