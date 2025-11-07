package io.github.atengk.service;

import io.github.atengk.model.UserTemplate;
import org.springframework.stereotype.Service;

/**
 * 原型克隆服务实现类
 * 模拟从模板克隆多个用户
 */
@Service
public class PrototypeServiceImpl implements PrototypeService {

    /** 模拟系统中的“原型模板对象” */
    private final UserTemplate userTemplate = new UserTemplate("TemplateUser", "Admin", "template@example.com");

    @Override
    public UserTemplate cloneUser(String username) {
        // 克隆模板对象
        UserTemplate clonedUser = userTemplate.clone();
        // 修改克隆体的个别属性
        return new UserTemplate(username, clonedUser.getRole(), username.toLowerCase() + "@example.com");
    }
}
