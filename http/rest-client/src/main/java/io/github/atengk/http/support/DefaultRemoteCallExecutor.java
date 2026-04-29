package io.github.atengk.http.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 默认远程调用执行器
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class DefaultRemoteCallExecutor implements RemoteCallExecutor {

    /**
     * 执行远程调用
     *
     * @param clientName 客户端名称
     * @param supplier   调用逻辑
     * @param <T>        返回类型
     * @return 调用结果
     */
    @Override
    public <T> T execute(String clientName, Supplier<T> supplier) {
        return supplier.get();
    }

}