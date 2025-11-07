package io.github.atengk.service;


import io.github.atengk.model.UserTemplate;

/**
 * 原型克隆服务接口
 */
public interface PrototypeService {

    /**
     * 克隆用户模板并自定义用户名
     *
     * @param username 用户名
     * @return 新的用户对象
     */
    UserTemplate cloneUser(String username);
}
