package io.github.atengk.basic.service;

import io.github.atengk.basic.annotation.DS;
import io.github.atengk.basic.holder.DataSourceContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（模拟多数据源）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserService {

    /**
     * 使用主库
     */
    @DS("master")
    public String getFromMaster() {
        return "当前数据源：" + DataSourceContextHolder.get();
    }

    /**
     * 使用从库
     */
    @DS("slave")
    public String getFromSlave() {
        return "当前数据源：" + DataSourceContextHolder.get();
    }
}
