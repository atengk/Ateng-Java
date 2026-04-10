package io.github.atengk.basic.service;

import io.github.atengk.basic.dao.UserDao;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 示例业务类
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserPageService {

    private final UserDao userDao;

    public UserPageService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<String> listUsers() {
        return userDao.queryUsers();
    }
}