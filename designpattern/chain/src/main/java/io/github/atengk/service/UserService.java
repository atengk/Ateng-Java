package io.github.atengk.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 模拟用户服务
 */
@Service
public class UserService {

    private static final Set<String> EXISTING_USERS = new HashSet<>();

    public boolean exists(String username) {
        return EXISTING_USERS.contains(username);
    }

    public void save(String username) {
        EXISTING_USERS.add(username);
    }
}
