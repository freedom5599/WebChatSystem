package com.chat.service;

import com.chat.common.Result;
import com.chat.entity.Friend;
import com.chat.entity.FriendGroup;
import com.chat.entity.FriendRequest;
import com.chat.mapper.FriendGroupMapper;
import com.chat.mapper.FriendMapper;
import com.chat.mapper.FriendRequestMapper;
import com.chat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendService {
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private FriendGroupMapper friendGroupMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Result<?> getFriendList(Long userId) {
        List<Friend> friends = friendMapper.findFriendsByUserId(userId);
        return Result.success(friends);
    }

    public Result<?> sendRequest(Long fromUserId, Long toUserId, String message) {
        if (fromUserId.equals(toUserId)) {
            return Result.error("不能添加自己为好友");
        }
        if (userMapper.findById(toUserId) == null) {
            return Result.error("目标用户不存在");
        }
        Friend exist = friendMapper.findByUserIdAndFriendId(fromUserId, toUserId);
        if (exist != null) {
            return Result.error("已经是好友了");
        }

        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setMessage(message);
        request.setStatus(0);
        friendRequestMapper.insert(request);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "friend_request");
        payload.put("requestId", request.getId());
        payload.put("fromUserId", fromUserId);
        payload.put("toUserId", toUserId);
        payload.put("message", message);
        messagingTemplate.convertAndSend("/topic/notification/" + toUserId, payload);

        return Result.success();
    }

    @Transactional
    public Result<?> handleRequest(Long requestId, Integer status, Long currentUserId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            return Result.error("申请不存在");
        }
        if (!request.getToUserId().equals(currentUserId)) {
            return Result.error("无权处理该申请");
        }
        if (request.getStatus() != 0) {
            return Result.error("该申请已处理");
        }
        friendRequestMapper.updateStatus(requestId, status);
        if (status == 1) {
            Friend friend1 = new Friend();
            friend1.setUserId(request.getFromUserId());
            friend1.setFriendId(request.getToUserId());
            List<FriendGroup> groups = friendGroupMapper.findByUserId(request.getFromUserId());
            if (!groups.isEmpty()) {
                friend1.setGroupId(groups.get(0).getId());
            }
            friendMapper.insert(friend1);

            Friend friend2 = new Friend();
            friend2.setUserId(request.getToUserId());
            friend2.setFriendId(request.getFromUserId());
            List<FriendGroup> groups2 = friendGroupMapper.findByUserId(request.getToUserId());
            if (!groups2.isEmpty()) {
                friend2.setGroupId(groups2.get(0).getId());
            }
            friendMapper.insert(friend2);
        }

        Map<String, Object> resultPayload = new HashMap<>();
        resultPayload.put("type", "friend_request_result");
        resultPayload.put("requestId", requestId);
        resultPayload.put("status", status);
        resultPayload.put("fromUserId", request.getFromUserId());
        resultPayload.put("toUserId", request.getToUserId());
        messagingTemplate.convertAndSend("/topic/notification/" + request.getFromUserId(), resultPayload);

        return Result.success();
    }

    public Result<?> getRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.findByToUserId(userId);
        return Result.success(requests);
    }

    @Transactional
    public Result<?> deleteFriend(Long userId, Long friendId) {
        friendMapper.delete(userId, friendId);
        friendMapper.delete(friendId, userId);
        return Result.success();
    }

    public Result<?> moveFriend(Long userId, Long friendId, Long groupId) {
        FriendGroup group = friendGroupMapper.findById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            return Result.error("分组不存在");
        }
        friendMapper.updateGroupId(userId, friendId, groupId);
        return Result.success();
    }

    public Result<?> getGroups(Long userId) {
        List<FriendGroup> groups = friendGroupMapper.findByUserId(userId);
        return Result.success(groups);
    }

    public Result<?> createGroup(Long userId, String groupName) {
        FriendGroup group = new FriendGroup();
        group.setUserId(userId);
        group.setGroupName(groupName);
        List<FriendGroup> existGroups = friendGroupMapper.findByUserId(userId);
        group.setSortOrder(existGroups.size() + 1);
        friendGroupMapper.insert(group);
        return Result.success(group);
    }

    public Result<?> deleteGroup(Long groupId, Long userId) {
        List<Friend> friends = friendMapper.findFriendsByGroupId(groupId);
        List<FriendGroup> groups = friendGroupMapper.findByUserId(userId);
        Long defaultGroupId = null;
        for (FriendGroup g : groups) {
            if (!g.getId().equals(groupId)) {
                defaultGroupId = g.getId();
                break;
            }
        }
        if (defaultGroupId != null) {
            for (Friend f : friends) {
                friendMapper.updateGroupId(userId, f.getFriendId(), defaultGroupId);
            }
        }
        friendGroupMapper.deleteById(groupId, userId);
        return Result.success();
    }
}
