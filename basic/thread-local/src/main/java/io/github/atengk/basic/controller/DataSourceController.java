package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多数据源测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class DataSourceController {

    private final UserService userService;

    public DataSourceController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * 测试主库
     *
     * http://localhost:8080/test/ds/master
     */
    @GetMapping("/test/ds/master")
    public String master() {
        return userService.getFromMaster();
    }

    /**
     * 测试从库
     *
     * http://localhost:8080/test/ds/slave
     */
    @GetMapping("/test/ds/slave")
    public String slave() {
        return userService.getFromSlave();
    }
}
