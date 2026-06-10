package com.chat.service;

import com.chat.common.Result;
import com.chat.entity.User;
import com.chat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result<?> register(User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }
        User exist = userMapper.findByUsername(user.getUsername());
        if (exist != null) {
            return Result.error("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (!StringUtils.hasText(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        if (!StringUtils.hasText(user.getAvatar())) {
            user.setAvatar("/img/default-avatar.png");
        }
        userMapper.insert(user);
        user.setPassword(null);
        return Result.success(user);
    }

    public Result<?> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.error("用户名和密码不能为空");
        }
        User user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        userMapper.updateStatus(user.getId(), 1);
        user.setStatus(1);
        user.setPassword(null);
        return Result.success(user);
    }

    public Result<?> logout(Long userId) {
        userMapper.updateStatus(userId, 0);
        return Result.success();
    }

    public Result<?> updateProfile(User user) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setNickname(user.getNickname());
        safeUser.setSignature(user.getSignature());
        safeUser.setAvatar(user.getAvatar());
        userMapper.update(safeUser);
        return Result.success();
    }

    public Result<?> getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    public User findById(Long id) {
        User user = userMapper.findById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public Result<?> searchByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Result.error("用户名不能为空");
        }
        User user = userMapper.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }
}
