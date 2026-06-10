var currentUser = null;
var stompClient = null;
var currentChatType = null;
var currentChatTarget = null;
var friendList = [];
var groupList = [];
var mediaRecorder = null;
var audioChunks = [];
var groupSubscription = null;
var reconnectDelay = 5000;

function init() {
    loadUserInfo();
    loadFriendList();
    loadGroupList();
    loadFriendRequests();
    loadGroupInvites();
}

function loadUserInfo() {
    fetch('/api/user/info').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            currentUser = data.data;
            document.getElementById('myNickname').textContent = currentUser.nickname;
            document.getElementById('mySignature').textContent = currentUser.signature || '';
            document.getElementById('myAvatar').src = currentUser.avatar || '/img/default-avatar.png';
            connectWebSocket();
        } else {
            window.location.href = '/login';
        }
    });
}

function connectWebSocket() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function(frame) {
        console.log('WebSocket连接成功: ' + frame);
        reconnectDelay = 5000;
        if (currentUser) {
            stompClient.subscribe('/topic/private/' + currentUser.id, function(message) {
                var msg = JSON.parse(message.body);
                onMessageReceived(msg);
            });
            stompClient.subscribe('/topic/user/status', function(message) {
                var statusMsg = JSON.parse(message.body);
                updateFriendStatus(statusMsg.userId, statusMsg.status);
            });
            stompClient.subscribe('/topic/notification/' + currentUser.id, function(message) {
                var notification = JSON.parse(message.body);
                if (notification.type === 'friend_request') {
                    loadFriendRequests();
                    alert('您有一条新的好友申请');
                } else if (notification.type === 'friend_request_result') {
                    if (notification.status === 1) {
                        loadFriendList();
                        alert('您的好友申请已被同意');
                    } else if (notification.status === 2) {
                        alert('您的好友申请已被拒绝');
                    }
                } else if (notification.type === 'group_invite') {
                    loadGroupInvites();
                    alert('您有一条新的群邀请：' + (notification.groupName || '未知群组'));
                }
            });
        }
    }, function(error) {
        console.log('WebSocket连接错误: ' + error);
        setTimeout(connectWebSocket, reconnectDelay);
        reconnectDelay = Math.min(reconnectDelay * 2, 60000);
    });
}

function onMessageReceived(msg) {
    if (msg.fromId === currentUser.id && !msg.groupId) {
        return;
    }
    if (currentChatType === 'private' && currentChatTarget &&
        msg.fromId === currentChatTarget.id && msg.toId === currentUser.id) {
        appendMessage(msg);
        fetch('/api/message/private/read?friendId=' + msg.fromId, {method: 'POST'});
    }
    if (currentChatType === 'group' && currentChatTarget && msg.groupId === currentChatTarget.id) {
        if (msg.fromId !== currentUser.id) {
            appendMessage(msg);
        }
    }
}

function updateFriendStatus(userId, status) {
    var dots = document.querySelectorAll('.friend-item .status-dot');
    dots.forEach(function(dot) {
        var friendItem = dot.closest('.friend-item');
        if (friendItem) {
            var onclickAttr = friendItem.getAttribute('onclick');
            if (onclickAttr && onclickAttr.indexOf('openPrivateChat(' + userId + ',') > -1) {
                dot.className = 'status-dot ' + (status === 1 ? 'status-online' : 'status-offline');
            }
        }
    });
}

function loadFriendList() {
    fetch('/api/friend/list').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            friendList = data.data;
            renderFriendList();
        }
    });
}

function renderFriendList() {
    var container = document.getElementById('friendList');
    var groups = {};
    friendList.forEach(function(f) {
        var gn = f.groupName || '默认分组';
        if (!groups[gn]) groups[gn] = [];
        groups[gn].push(f);
    });
    var html = '';
    Object.keys(groups).forEach(function(gn) {
        html += '<div class="friend-group-header" onclick="toggleGroup(this)">▶ ' + gn + ' (' + groups[gn].length + ')</div>';
        html += '<div class="friend-group-items" style="display:block">';
        groups[gn].forEach(function(f) {
            var activeClass = (currentChatType === 'private' && currentChatTarget && currentChatTarget.id === f.friendId) ? ' active' : '';
            var statusClass = f.friendStatus === 1 ? 'status-online' : 'status-offline';
            var displayName = f.remark || f.friendNickname || '用户' + f.friendId;
            html += '<div class="friend-item' + activeClass + '" onclick="openPrivateChat(' + f.friendId + ', \'' + escapeHtml(displayName) + '\', \'' + (f.friendAvatar || '') + '\')">';
            html += '<img class="avatar" src="' + (f.friendAvatar || '/img/default-avatar.png') + '" alt="">';
            html += '<div class="info"><div class="name">' + escapeHtml(displayName) + '</div>';
            if (f.remark && f.friendNickname) {
                html += '<div class="remark">昵称: ' + escapeHtml(f.friendNickname) + '</div>';
            }
            html += '</div>';
            html += '<div class="status-dot ' + statusClass + '"></div>';
            html += '</div>';
        });
        html += '</div>';
    });
    if (friendList.length === 0) {
        html = '<div class="loading">暂无好友，点击上方"添加"按钮添加好友</div>';
    }
    container.innerHTML = html;
}

function toggleGroup(el) {
    var items = el.nextElementSibling;
    if (items.style.display === 'none') {
        items.style.display = 'block';
        el.textContent = el.textContent.replace('▶', '▼');
    } else {
        items.style.display = 'none';
        el.textContent = el.textContent.replace('▼', '▶');
    }
}

function loadGroupList() {
    fetch('/api/group/my').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            groupList = data.data;
            renderGroupList();
        }
    });
}

function renderGroupList() {
    var container = document.getElementById('groupList');
    var html = '';
    html += '<div class="friend-item" onclick="showJoinGroupModal()" style="color:#667eea;font-size:13px">';
    html += '<span style="margin-right:8px">➕</span>加入群聊</div>';
    groupList.forEach(function(g) {
        var activeClass = (currentChatType === 'group' && currentChatTarget && currentChatTarget.id === g.id) ? ' active' : '';
        html += '<div class="group-item' + activeClass + '" onclick="openGroupChat(' + g.id + ', \'' + escapeHtml(g.groupName) + '\')">';
        html += '<img class="avatar" src="' + (g.avatar || '/img/default-group.png') + '" alt="">';
        html += '<div class="info"><div class="name">' + escapeHtml(g.groupName) + '</div>';
        html += '<div class="remark">群ID: ' + g.id + ' · ' + (g.memberCount || 0) + '人</div></div></div>';
    });
    if (groupList.length === 0) {
        html += '<div class="loading">暂无群聊，点击上方"创建"按钮创建群聊</div>';
    }
    container.innerHTML = html;
}

function loadFriendRequests() {
    fetch('/api/friend/requests').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            renderFriendRequests(data.data);
        }
    });
}

function renderFriendRequests(requests) {
    var container = document.getElementById('requestList');
    var badge = document.getElementById('requestBadge');
    if (requests && requests.length > 0) {
        badge.textContent = requests.length;
        badge.style.display = 'inline';
        var html = '';
        requests.forEach(function(r) {
            html += '<div class="request-item">';
            html += '<div class="request-user"><img class="avatar" src="' + (r.fromAvatar || '/img/default-avatar.png') + '" alt="">';
            html += '<span class="name">' + escapeHtml(r.fromNickname || r.fromUsername) + '</span></div>';
            html += '<div class="request-msg">验证信息: ' + escapeHtml(r.message || '无') + '</div>';
            html += '<div class="request-actions">';
            html += '<button class="btn-accept" onclick="handleFriendRequest(' + r.id + ', 1)">同意</button>';
            html += '<button class="btn-reject" onclick="handleFriendRequest(' + r.id + ', 2)">拒绝</button>';
            html += '</div></div>';
        });
        container.innerHTML = html;
    } else {
        badge.style.display = 'none';
        container.innerHTML = '<div class="loading">暂无好友申请</div>';
    }
}

function handleFriendRequest(requestId, status) {
    fetch('/api/friend/handle', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({requestId: requestId, status: status})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            loadFriendRequests();
            loadFriendList();
            alert(status === 1 ? '已同意好友申请' : '已拒绝好友申请');
        } else {
            alert(data.msg);
        }
    });
}

function loadGroupInvites() {
    fetch('/api/group/invites').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            renderGroupInvites(data.data);
        }
    });
}

function renderGroupInvites(invites) {
    var container = document.getElementById('groupInviteList');
    if (!container) return;
    if (invites && invites.length > 0) {
        var pending = invites.filter(function(i) { return i.status === 0; });
        var html = '';
        invites.forEach(function(i) {
            var statusText = '';
            var actionsHtml = '';
            if (i.status === 0) {
                actionsHtml = '<button class="btn-accept" onclick="acceptGroupInvite(' + i.id + ')">接受</button>'
                    + '<button class="btn-reject" onclick="rejectGroupInvite(' + i.id + ')">拒绝</button>';
                statusText = '待处理';
            } else if (i.status === 1) {
                statusText = '已接受';
            } else {
                statusText = '已拒绝';
            }
            html += '<div class="request-item">';
            html += '<div class="request-user"><img class="avatar" src="' + (i.fromUserAvatar || '/img/default-avatar.png') + '" alt="">';
            html += '<span class="name">' + escapeHtml(i.fromUserNickname || '用户' + i.fromUserId) + '</span></div>';
            html += '<div class="request-msg">邀请加入群聊: <b>' + escapeHtml(i.groupName || '') + '</b></div>';
            html += '<div class="request-msg">状态: ' + statusText + '</div>';
            if (actionsHtml) {
                html += '<div class="request-actions">' + actionsHtml + '</div>';
            }
            html += '</div>';
        });
        container.innerHTML = html;
    } else {
        container.innerHTML = '<div class="loading">暂无群邀请</div>';
    }
}

function acceptGroupInvite(inviteId) {
    fetch('/api/group/invite/accept', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({inviteId: inviteId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            loadGroupInvites();
            loadGroupList();
            alert('已接受群邀请');
        } else {
            alert(data.msg);
        }
    });
}

function rejectGroupInvite(inviteId) {
    fetch('/api/group/invite/reject', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({inviteId: inviteId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            loadGroupInvites();
            alert('已拒绝群邀请');
        } else {
            alert(data.msg);
        }
    });
}

function showGroupInviteModal(groupId, groupName) {
    document.getElementById('inviteGroupId').value = groupId;
    document.getElementById('inviteGroupTitle').textContent = '邀请好友加入「' + groupName + '」';
    loadFriendsForInvite(groupId);
    document.getElementById('groupInviteFriendModal').style.display = 'flex';
}

function loadFriendsForInvite(groupId) {
    fetch('/api/friend/list').then(function(res) { return res.json(); }).then(function(data) {
        var html = '';
        if (data.code === 200 && data.data && data.data.length > 0) {
            data.data.forEach(function(f) {
                var displayName = f.remark || f.friendNickname || '用户' + f.friendId;
                html += '<div class="member-item">';
                html += '<img class="avatar" src="' + (f.friendAvatar || '/img/default-avatar.png') + '" alt="">';
                html += '<span class="name">' + escapeHtml(displayName) + '</span>';
                html += '<button class="btn-accept" style="font-size:12px;padding:3px 10px" onclick="inviteFriendToGroup(' + f.friendId + ')">邀请</button>';
                html += '</div>';
            });
        } else {
            html = '<div class="loading">暂无可邀请的好友</div>';
        }
        document.getElementById('inviteFriendList').innerHTML = html;
    });
}

function inviteFriendToGroup(friendId) {
    var groupId = parseInt(document.getElementById('inviteGroupId').value);
    fetch('/api/group/invite', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupId: groupId, userId: friendId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            alert('邀请已发送');
            closeModal('groupInviteFriendModal');
        } else {
            alert(data.msg);
        }
    });
}

function switchTab(tab) {
    document.querySelectorAll('.tab').forEach(function(t) { t.classList.remove('active'); });
    document.querySelectorAll('.tab-content').forEach(function(t) { t.classList.remove('active'); });
    document.querySelector('[data-tab="' + tab + '"]').classList.add('active');
    document.getElementById(tab + 'Tab').classList.add('active');
}

function openPrivateChat(friendId, name, avatar) {
    currentChatType = 'private';
    currentChatTarget = {id: friendId, nickname: name, avatar: avatar};
    showChatArea();
    document.getElementById('chatTargetName').textContent = name;
    document.getElementById('chatTargetDesc').textContent = '私聊';
    document.getElementById('chatTargetAvatar').src = avatar || '/img/default-avatar.png';
    document.getElementById('groupInfoBtn').style.display = 'none';
    document.getElementById('friendManageBtn').style.display = 'inline';
    document.querySelectorAll('.private-only').forEach(function(el) { el.style.display = 'inline-block'; });
    loadPrivateMessages(friendId);
    fetch('/api/message/private/read?friendId=' + friendId, {method: 'POST'});
    renderFriendList();
}

function openGroupChat(groupId, groupName) {
    if (groupSubscription) {
        groupSubscription.unsubscribe();
    }
    currentChatType = 'group';
    currentChatTarget = {id: groupId, groupName: groupName};
    showChatArea();
    document.getElementById('chatTargetName').textContent = groupName;
    document.getElementById('chatTargetDesc').innerHTML = '群聊 · 群ID: <span class="group-id-text" title="点击复制群ID" onclick="copyGroupId(' + groupId + ', event)">' + groupId + '</span>';
    document.getElementById('chatTargetAvatar').src = '/img/default-group.png';
    document.getElementById('groupInfoBtn').style.display = 'inline';
    document.getElementById('friendManageBtn').style.display = 'none';
    document.querySelectorAll('.private-only').forEach(function(el) { el.style.display = 'none'; });
    loadGroupMessages(groupId);
    renderGroupList();
    if (stompClient) {
        groupSubscription = stompClient.subscribe('/topic/group/' + groupId, function(message) {
            var msg = JSON.parse(message.body);
            onMessageReceived(msg);
        });
    }
}

function showChatArea() {
    document.getElementById('chatPlaceholder').style.display = 'none';
    document.getElementById('chatHeader').style.display = 'flex';
    document.getElementById('chatMessages').style.display = 'block';
    document.getElementById('chatInput').style.display = 'block';
}

function loadPrivateMessages(friendId) {
    fetch('/api/message/private/history?friendId=' + friendId).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            renderMessages(data.data, 'private');
        }
    });
}

function loadGroupMessages(groupId) {
    fetch('/api/group/messages/' + groupId).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            renderMessages(data.data, 'group');
        }
    });
}

function buildMessageBodyHtml(msg) {
    if (msg.msgType === 1 && msg.voiceUrl) {
        return '<div class="message-bubble voice-bubble" onclick="playVoice(\'' + escapeAttr(msg.voiceUrl) + '\')">' +
            '<span class="voice-icon">🎤</span><span class="voice-duration">语音消息</span></div>';
    }
    if (msg.msgType === 2 && msg.voiceUrl) {
        return '<div class="message-bubble image-bubble">' +
            '<img class="chat-image" src="' + escapeAttr(msg.voiceUrl) + '" alt="图片" onclick="previewImage(\'' + escapeAttr(msg.voiceUrl) + '\')"></div>';
    }
    if (msg.msgType === 3 && msg.voiceUrl) {
        var fileName = escapeHtml(msg.content || '文件');
        return '<div class="message-bubble file-bubble">' +
            '<a class="file-link" href="' + escapeAttr(msg.voiceUrl) + '" download target="_blank">' +
            '<span class="file-icon">📎</span><span class="file-name">' + fileName + '</span></a></div>';
    }
    return '<div class="message-bubble">' + escapeHtml(msg.content) + '</div>';
}

function renderMessages(messages, type) {
    var container = document.getElementById('chatMessages');
    var html = '';
    if (messages && messages.length > 0) {
        messages.forEach(function(msg) {
            var isSelf = msg.fromId === currentUser.id;
            var senderName = msg.fromNickname || '用户' + msg.fromId;
            var avatar = msg.fromAvatar || '/img/default-avatar.png';
            var time = msg.createTime ? formatTime(msg.createTime) : '';
            html += '<div class="message-item' + (isSelf ? ' self' : '') + '">';
            html += '<img class="avatar" src="' + avatar + '" alt="">';
            html += '<div class="message-content">';
            if (!isSelf) {
                html += '<div class="message-sender">' + escapeHtml(senderName) + '</div>';
            }
            html += buildMessageBodyHtml(msg);
            html += '<div class="message-time">' + time + '</div>';
            html += '</div></div>';
        });
    }
    container.innerHTML = html;
    container.scrollTop = container.scrollHeight;
}

function appendMessage(msg) {
    var container = document.getElementById('chatMessages');
    var isSelf = msg.fromId === currentUser.id;
    var senderName = msg.fromNickname || '用户' + msg.fromId;
    var avatar = msg.fromAvatar || '/img/default-avatar.png';
    var time = msg.time || formatTime(new Date());
    var html = '<div class="message-item' + (isSelf ? ' self' : '') + '">';
    html += '<img class="avatar" src="' + avatar + '" alt="">';
    html += '<div class="message-content">';
    if (!isSelf) {
        html += '<div class="message-sender">' + escapeHtml(senderName) + '</div>';
    }
    html += buildMessageBodyHtml(msg);
    html += '<div class="message-time">' + time + '</div>';
    html += '</div></div>';
    container.insertAdjacentHTML('beforeend', html);
    container.scrollTop = container.scrollHeight;
}

function sendMessage() {
    var input = document.getElementById('messageInput');
    var content = input.value.trim();
    if (!content) return;
    if (!currentChatTarget) return;

    var now = new Date();
    var timeStr = now.getFullYear() + '-' + ('0' + (now.getMonth() + 1)).slice(-2) + '-' + ('0' + now.getDate()).slice(-2) + ' ' + ('0' + now.getHours()).slice(-2) + ':' + ('0' + now.getMinutes()).slice(-2) + ':' + ('0' + now.getSeconds()).slice(-2);

    var chatMsg = {
        fromId: currentUser.id,
        fromNickname: currentUser.nickname,
        fromAvatar: currentUser.avatar,
        content: content,
        msgType: 0,
        time: timeStr
    };

    appendMessage(chatMsg);

    if (currentChatType === 'private') {
        chatMsg.type = 'PRIVATE';
        chatMsg.toId = currentChatTarget.id;
        stompClient.send('/app/chat.private', {}, JSON.stringify(chatMsg));
    } else {
        chatMsg.type = 'GROUP';
        chatMsg.groupId = currentChatTarget.id;
        stompClient.send('/app/chat.group', {}, JSON.stringify(chatMsg));
    }
    input.value = '';
}

function handleInputKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function toggleVoiceRecord() {
    var voiceArea = document.getElementById('voiceArea');
    var inputArea = document.querySelector('.input-area');
    if (voiceArea.style.display === 'none') {
        voiceArea.style.display = 'block';
        inputArea.style.display = 'none';
    } else {
        voiceArea.style.display = 'none';
        inputArea.style.display = 'flex';
    }
}

function startRecording() {
    navigator.mediaDevices.getUserMedia({audio: true}).then(function(stream) {
        mediaRecorder = new MediaRecorder(stream);
        audioChunks = [];
        mediaRecorder.ondataavailable = function(e) { audioChunks.push(e.data); };
        mediaRecorder.onstop = function() {
            var blob = new Blob(audioChunks, {type: 'audio/webm'});
            sendVoiceMessage(blob);
            stream.getTracks().forEach(function(t) { t.stop(); });
        };
        mediaRecorder.start();
        document.getElementById('voiceRecordBtn').classList.add('recording');
        document.getElementById('voiceHint').textContent = '录音中...松开停止';
    }).catch(function(err) {
        alert('无法访问麦克风: ' + err.message);
    });
}

function stopRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
        document.getElementById('voiceRecordBtn').classList.remove('recording');
        document.getElementById('voiceHint').textContent = '按住按钮开始录音';
    }
}

function sendVoiceMessage(blob) {
    if (!currentChatTarget) {
        alert('请先选择聊天对象');
        return;
    }
    if (!stompClient || !stompClient.connected) {
        alert('网络连接中断，请稍后重试');
        return;
    }
    if (!blob || blob.size === 0) {
        alert('录音时间太短，请重新录制');
        return;
    }
    var formData = new FormData();
    formData.append('file', blob, 'voice.webm');
    fetch('/api/file/upload', {
        method: 'POST',
        body: formData
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            var now = new Date();
            var timeStr = now.getFullYear() + '-' + ('0' + (now.getMonth() + 1)).slice(-2) + '-' + ('0' + now.getDate()).slice(-2) + ' ' + ('0' + now.getHours()).slice(-2) + ':' + ('0' + now.getMinutes()).slice(-2) + ':' + ('0' + now.getSeconds()).slice(-2);

            var chatMsg = {
                fromId: currentUser.id,
                fromNickname: currentUser.nickname,
                fromAvatar: currentUser.avatar,
                content: '[语音]',
                msgType: 1,
                voiceUrl: data.data,
                time: timeStr
            };

            // 立即在本地显示语音消息
            appendMessage(chatMsg);

            if (currentChatType === 'private') {
                chatMsg.type = 'PRIVATE';
                chatMsg.toId = currentChatTarget.id;
                stompClient.send('/app/chat.private', {}, JSON.stringify(chatMsg));
            } else {
                chatMsg.type = 'GROUP';
                chatMsg.groupId = currentChatTarget.id;
                stompClient.send('/app/chat.group', {}, JSON.stringify(chatMsg));
            }
        } else {
            alert('语音上传失败：' + data.msg);
        }
    }).catch(function(err) {
        alert('语音发送失败：' + err.message);
    });
}

function playVoice(url) {
    var audio = new Audio(url);
    audio.play();
}

function previewImage(url) {
    document.getElementById('previewImage').src = url;
    document.getElementById('imagePreviewModal').style.display = 'flex';
}

function sendImageFile() {
    var fileInput = document.getElementById('imageFile');
    if (!fileInput.files[0]) return;
    var file = fileInput.files[0];
    if (!file.type.startsWith('image/')) {
        alert('请选择图片文件');
        fileInput.value = '';
        return;
    }
    if (file.size > 10 * 1024 * 1024) {
        alert('图片大小不能超过10MB');
        fileInput.value = '';
        return;
    }
    sendAttachment(file, 2, '[图片]');
    fileInput.value = '';
}

function sendAttachFile() {
    var fileInput = document.getElementById('attachFile');
    if (!fileInput.files[0]) return;
    var file = fileInput.files[0];
    if (file.size > 10 * 1024 * 1024) {
        alert('文件大小不能超过10MB');
        fileInput.value = '';
        return;
    }
    sendAttachment(file, 3, file.name);
    fileInput.value = '';
}

function sendAttachment(file, msgType, content) {
    if (!currentChatTarget) {
        alert('请先选择聊天对象');
        return;
    }
    if (currentChatType !== 'private') {
        alert('图片和文件仅支持私聊发送');
        return;
    }
    if (!stompClient || !stompClient.connected) {
        alert('网络连接中断，请稍后重试');
        return;
    }
    var formData = new FormData();
    formData.append('file', file);
    fetch('/api/file/upload', {
        method: 'POST',
        body: formData
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            var now = new Date();
            var timeStr = now.getFullYear() + '-' + ('0' + (now.getMonth() + 1)).slice(-2) + '-' + ('0' + now.getDate()).slice(-2) + ' ' + ('0' + now.getHours()).slice(-2) + ':' + ('0' + now.getMinutes()).slice(-2) + ':' + ('0' + now.getSeconds()).slice(-2);
            var chatMsg = {
                fromId: currentUser.id,
                fromNickname: currentUser.nickname,
                fromAvatar: currentUser.avatar,
                content: content,
                msgType: msgType,
                voiceUrl: data.data,
                time: timeStr,
                type: 'PRIVATE',
                toId: currentChatTarget.id
            };
            appendMessage(chatMsg);
            stompClient.send('/app/chat.private', {}, JSON.stringify(chatMsg));
        } else {
            alert('文件上传失败：' + data.msg);
        }
    }).catch(function(err) {
        alert('文件发送失败：' + err.message);
    });
}

function logout() {
    if (confirm('确定要退出登录吗？')) {
        fetch('/api/user/logout', {method: 'POST'}).then(function() {
            window.location.href = '/login';
        });
    }
}

function showProfileModal() {
    document.getElementById('profileNickname').value = currentUser.nickname || '';
    document.getElementById('profileSignature').value = currentUser.signature || '';
    document.getElementById('profileAvatar').src = currentUser.avatar || '/img/default-avatar.png';
    document.getElementById('profileModal').style.display = 'flex';
}

function saveProfile() {
    var nickname = document.getElementById('profileNickname').value;
    var signature = document.getElementById('profileSignature').value;
    fetch('/api/user/update', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({nickname: nickname, signature: signature})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            currentUser = data.data;
            document.getElementById('myNickname').textContent = currentUser.nickname;
            document.getElementById('mySignature').textContent = currentUser.signature || '';
            closeModal('profileModal');
            alert('保存成功');
        } else {
            alert(data.msg);
        }
    });
}

function uploadAvatar() {
    var fileInput = document.getElementById('avatarFile');
    if (!fileInput.files[0]) return;
    var formData = new FormData();
    formData.append('file', fileInput.files[0]);
    fetch('/api/file/upload', {
        method: 'POST',
        body: formData
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            fetch('/api/user/update', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({avatar: data.data})
            }).then(function(res) { return res.json(); }).then(function(d) {
                if (d.code === 200) {
                    currentUser = d.data;
                    var avatarUrl = currentUser.avatar + '?t=' + Date.now();
                    document.getElementById('myAvatar').src = avatarUrl;
                    document.getElementById('profileAvatar').src = avatarUrl;
                    alert('头像更换成功');
                } else {
                    alert(d.msg || '头像保存失败');
                }
                fileInput.value = '';
            }).catch(function(err) {
                alert('头像保存失败：' + err.message);
                fileInput.value = '';
            });
        } else {
            alert(data.msg || '头像上传失败');
            fileInput.value = '';
        }
    }).catch(function(err) {
        alert('头像上传失败：' + err.message);
        fileInput.value = '';
    });
}

function showAddFriendModal() {
    document.getElementById('addFriendUsername').value = '';
    document.getElementById('addFriendMessage').value = '';
    document.getElementById('addFriendModal').style.display = 'flex';
}

function sendFriendRequest() {
    var username = document.getElementById('addFriendUsername').value.trim();
    var message = document.getElementById('addFriendMessage').value.trim();
    if (!username) { alert('请输入用户名'); return; }
    fetch('/api/user/search?username=' + encodeURIComponent(username)).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            var toUserId = data.data.id;
            fetch('/api/friend/request', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({toUserId: toUserId, message: message})
            }).then(function(res2) { return res2.json(); }).then(function(data2) {
                if (data2.code === 200) {
                    closeModal('addFriendModal');
                    alert('好友申请已发送');
                } else {
                    alert(data2.msg);
                }
            });
        } else {
            alert(data.msg || '用户不存在');
        }
    }).catch(function() {
        alert('发送失败，请检查用户名');
    });
}

function handleFriendSearch(event) {
    if (event.key === 'Enter') {
        var username = document.getElementById('searchFriendInput').value.trim();
        if (username) {
            showAddFriendModal();
            document.getElementById('addFriendUsername').value = username;
        }
    }
}

function showCreateGroupModal() {
    document.getElementById('createGroupName').value = '';
    document.getElementById('createGroupDesc').value = '';
    document.getElementById('createGroupModal').style.display = 'flex';
}

function createGroup() {
    var name = document.getElementById('createGroupName').value.trim();
    if (!name) { alert('请输入群名称'); return; }
    var desc = document.getElementById('createGroupDesc').value.trim();
    fetch('/api/group/create', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupName: name, description: desc})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            closeModal('createGroupModal');
            loadGroupList();
            var newGroupId = data.data && data.data.id ? data.data.id : '';
            alert('群聊创建成功！群ID: ' + newGroupId + '（可分享给好友加入）');
        } else {
            alert(data.msg);
        }
    });
}

function showJoinGroupModal() {
    document.getElementById('joinGroupId').value = '';
    document.getElementById('joinGroupModal').style.display = 'flex';
}

function joinGroup() {
    var groupId = document.getElementById('joinGroupId').value.trim();
    if (!groupId) { alert('请输入群组ID'); return; }
    fetch('/api/group/join', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupId: groupId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            closeModal('joinGroupModal');
            loadGroupList();
            alert('加入成功');
        } else {
            alert(data.msg);
        }
    });
}

function leaveGroup() {
    if (!currentChatTarget || currentChatType !== 'group') return;
    if (confirm('确定要退出该群聊吗？')) {
        fetch('/api/group/leave', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({groupId: currentChatTarget.id})
        }).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                closeModal('groupInfoModal');
                loadGroupList();
                currentChatType = null;
                currentChatTarget = null;
                document.getElementById('chatPlaceholder').style.display = 'flex';
                document.getElementById('chatHeader').style.display = 'none';
                document.getElementById('chatMessages').style.display = 'none';
                document.getElementById('chatInput').style.display = 'none';
                alert(data.data || '已退出群聊');
            } else {
                alert(data.msg);
            }
        });
    }
}

function showGroupInfoModal() {
    if (!currentChatTarget || currentChatType !== 'group') return;
    fetch('/api/group/info/' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(groupData) {
        if (groupData.code !== 200) { alert(groupData.msg); return; }
        var groupInfo = groupData.data;
        var isOwner = groupInfo.ownerId === currentUser.id;

        fetch('/api/group/members/' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                var html = '<div class="group-info-summary">';
                html += '<div class="group-info-row"><span class="label">群名称</span><span>' + escapeHtml(groupInfo.groupName) + '</span></div>';
                html += '<div class="group-info-row"><span class="label">群ID</span><span class="group-id-copy" title="点击复制" onclick="copyGroupId(' + groupInfo.id + ', event)">' + groupInfo.id + ' 📋</span></div>';
                if (groupInfo.description) {
                    html += '<div class="group-info-row"><span class="label">群简介</span><span>' + escapeHtml(groupInfo.description) + '</span></div>';
                }
                html += '</div>';
                html += '<h4 style="margin:12px 0 10px">群成员 (' + data.data.length + '人)</h4>';
                if (isOwner) {
                    html += '<button class="btn-confirm" style="margin-bottom:10px;font-size:12px;padding:6px 12px" onclick="showInviteFriendModal()">邀请好友加入</button>';
                }
                data.data.forEach(function(m) {
                    var roleText = m.role === 2 ? '群主' : (m.role === 1 ? '管理员' : '成员');
                    html += '<div class="member-item">';
                    html += '<img class="avatar" src="' + (m.userAvatar || '/img/default-avatar.png') + '" alt="">';
                    html += '<span class="name">' + escapeHtml(m.userNickname || '用户' + m.userId) + '</span>';
                    html += '<span class="role">' + roleText + '</span>';
                    if (isOwner && m.role !== 2) {
                        html += '<button class="btn-kick" onclick="kickMember(' + m.userId + ')">踢出</button>';
                    }
                    html += '</div>';
                });
                document.getElementById('groupInfoContent').innerHTML = html;
                document.getElementById('groupInfoModal').style.display = 'flex';
            }
        });
    });
}

function showInviteFriendModal() {
    closeModal('groupInfoModal');
    var html = '';
    friendList.forEach(function(f) {
        var displayName = f.remark || f.friendNickname || '用户' + f.friendId;
        html += '<div class="member-item">';
        html += '<img class="avatar" src="' + (f.friendAvatar || '/img/default-avatar.png') + '" alt="">';
        html += '<span class="name">' + escapeHtml(displayName) + '</span>';
        html += '<button class="btn-confirm" style="font-size:12px;padding:4px 10px" onclick="inviteFriend(' + f.friendId + ')">邀请</button>';
        html += '</div>';
    });
    if (friendList.length === 0) {
        html = '<div class="loading">暂无好友可邀请</div>';
    }
    document.getElementById('inviteFriendContent').innerHTML = html;
    document.getElementById('inviteFriendModal').style.display = 'flex';
}

function inviteFriend(friendId) {
    fetch('/api/group/invite', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupId: currentChatTarget.id, userId: friendId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            alert('邀请成功');
            closeModal('inviteFriendModal');
            loadGroupList();
        } else {
            alert(data.msg);
        }
    });
}

function kickMember(userId) {
    if (!confirm('确定要将该成员移出群聊吗？')) return;
    fetch('/api/group/kick', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupId: currentChatTarget.id, userId: userId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            alert('已移出');
            showGroupInfoModal();
        } else {
            alert(data.msg);
        }
    });
}

function showFriendManageModal() {
    if (!currentChatTarget || currentChatType !== 'private') return;
    var friend = friendList.find(function(f) { return f.friendId === currentChatTarget.id; });
    if (friend) {
        document.getElementById('friendRemark').value = friend.remark || '';
    }
    loadFriendGroups();
    document.getElementById('friendManageModal').style.display = 'flex';
}

function loadFriendGroups() {
    fetch('/api/friend/groups').then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            var select = document.getElementById('friendGroupSelect');
            var html = '';
            data.data.forEach(function(g) {
                html += '<option value="' + g.id + '">' + escapeHtml(g.groupName) + '</option>';
            });
            select.innerHTML = html;
        }
    });
}

function saveFriendManage() {
    if (!currentChatTarget) return;
    var groupId = document.getElementById('friendGroupSelect').value;
    fetch('/api/friend/move', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({friendId: currentChatTarget.id, groupId: groupId})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            loadFriendList();
            closeModal('friendManageModal');
            alert('保存成功');
        } else {
            alert(data.msg);
        }
    });
}

function deleteFriend() {
    if (!currentChatTarget || currentChatType !== 'private') return;
    if (confirm('确定要删除该好友吗？删除后聊天记录将保留。')) {
        fetch('/api/friend/delete?friendId=' + currentChatTarget.id, {
            method: 'DELETE'
        }).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                closeModal('friendManageModal');
                loadFriendList();
                currentChatType = null;
                currentChatTarget = null;
                document.getElementById('chatPlaceholder').style.display = 'flex';
                document.getElementById('chatHeader').style.display = 'none';
                document.getElementById('chatMessages').style.display = 'none';
                document.getElementById('chatInput').style.display = 'none';
                alert('已删除好友');
            } else {
                alert(data.msg);
            }
        });
    }
}

function showHistoryModal() {
    if (!currentChatTarget) return;
    if (currentChatType === 'private') {
        fetch('/api/message/private/history?friendId=' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                renderHistoryMessages(data.data, 'private');
                document.getElementById('historyModal').style.display = 'flex';
            }
        });
    } else {
        fetch('/api/group/messages/' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                renderHistoryMessages(data.data, 'group');
                document.getElementById('historyModal').style.display = 'flex';
            }
        });
    }
}

function renderHistoryMessages(messages, type) {
    var container = document.getElementById('historyMessages');
    var html = '';
    if (messages && messages.length > 0) {
        messages.forEach(function(msg) {
            var isSelf = msg.fromId === currentUser.id;
            var senderName = msg.fromNickname || '用户' + msg.fromId;
            var time = msg.createTime ? formatTime(msg.createTime) : '';
            html += '<div class="message-item' + (isSelf ? ' self' : '') + '">';
            html += '<div class="message-content">';
            html += '<div class="message-sender">' + escapeHtml(senderName) + ' ' + time + '</div>';
            html += buildMessageBodyHtml(msg);
            html += '</div></div>';
        });
    } else {
        html = '<div class="loading">暂无聊天记录</div>';
    }
    container.innerHTML = html;
}

function exportChat() {
    if (!currentChatTarget) return;
    if (currentChatType === 'private') {
        fetch('/api/message/private/export?friendId=' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                downloadTextFile('聊天记录.txt', data.data);
            }
        });
    } else {
        fetch('/api/group/export/' + currentChatTarget.id).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                downloadTextFile('群聊记录.txt', data.data);
            }
        });
    }
}

function downloadTextFile(filename, content) {
    var blob = new Blob([content], {type: 'text/plain;charset=utf-8'});
    var url = URL.createObjectURL(blob);
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

function createFriendGroup() {
    var name = document.getElementById('newGroupName').value.trim();
    if (!name) { alert('请输入分组名称'); return; }
    fetch('/api/friend/group/add', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({groupName: name})
    }).then(function(res) { return res.json(); }).then(function(data) {
        if (data.code === 200) {
            document.getElementById('newGroupName').value = '';
            loadFriendGroups();
            loadFriendList();
            alert('分组创建成功');
        } else {
            alert(data.msg);
        }
    });
}

function deleteFriendGroup(groupId) {
    if (confirm('确定要删除该分组吗？分组内好友将移至默认分组')) {
        fetch('/api/friend/group/delete?groupId=' + groupId, {
            method: 'DELETE'
        }).then(function(res) { return res.json(); }).then(function(data) {
            if (data.code === 200) {
                loadFriendList();
                loadFriendGroups();
                alert('分组已删除');
            } else {
                alert(data.msg);
            }
        });
    }
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(text));
    return div.innerHTML;
}

function copyGroupId(groupId, event) {
    if (event) {
        event.stopPropagation();
    }
    var text = String(groupId);
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(function() {
            alert('群ID已复制: ' + text);
        }).catch(function() {
            alert('群ID: ' + text);
        });
    } else {
        var input = document.createElement('input');
        input.value = text;
        document.body.appendChild(input);
        input.select();
        try {
            document.execCommand('copy');
            alert('群ID已复制: ' + text);
        } catch (e) {
            alert('群ID: ' + text);
        }
        document.body.removeChild(input);
    }
}

function escapeAttr(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function formatTime(time) {
    if (typeof time === 'string') {
        return time.replace('T', ' ').substring(0, 19);
    }
    if (time instanceof Date) {
        var y = time.getFullYear();
        var m = ('0' + (time.getMonth() + 1)).slice(-2);
        var d = ('0' + time.getDate()).slice(-2);
        var h = ('0' + time.getHours()).slice(-2);
        var min = ('0' + time.getMinutes()).slice(-2);
        var s = ('0' + time.getSeconds()).slice(-2);
        return y + '-' + m + '-' + d + ' ' + h + ':' + min + ':' + s;
    }
    return '';
}

window.onload = init;
