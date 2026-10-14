package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.UserRequest;
import com.example.workadmin.entity.User;
import com.example.workadmin.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> list(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        }
        return userRepository.findByUsernameContainingOrderByIdAsc(keyword.trim());
    }

    @Transactional
    public User add(UserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPhone(request.getPhone());
        user.setRole(normalizeRole(request.getRole(), "user"));
        return userRepository.save(user);
    }

    @Transactional
    public User edit(UserRequest request) {
        if (request.getId() == null) {
            throw new BizException(400, "缺少用户 id");
        }
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        user.setUsername(request.getUsername().trim());
        user.setPhone(request.getPhone());
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BizException(400, "缺少用户 id");
        }
        if (!userRepository.existsById(id)) {
            throw new BizException(404, "用户不存在");
        }
        userRepository.deleteById(id);
    }

    private String normalizeRole(String role, String defaultRole) {
        return (role == null || role.isBlank()) ? defaultRole : role;
    }
}
