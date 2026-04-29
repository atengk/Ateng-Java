package io.github.atengk.http.client;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import io.github.atengk.http.exception.RemoteCallException;
import io.github.atengk.http.support.RemoteCallExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 示例远程接口客户端
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Slf4j
@Component
public class DemoApiClient {

    private final RestClient restClient;

    private final RemoteCallExecutor remoteCallExecutor;

    public DemoApiClient(
            @Qualifier("defaultRestClient") RestClient restClient,
            RemoteCallExecutor remoteCallExecutor
    ) {
        this.restClient = restClient;
        this.remoteCallExecutor = remoteCallExecutor;
    }

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    public UserDetailResp getUserDetail(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DEFAULT, () -> restClient.get()
                    .uri("/api/users/{userId}", userId)
                    .retrieve()
                    .body(UserDetailResp.class));
        } catch (RemoteCallException ex) {
            log.warn(
                    "查询远程用户详情失败，userId={}，status={}，responseBody={}",
                    userId,
                    ex.getStatusCode(),
                    ex.getResponseBody()
            );
            throw ex;
        }
    }

    /**
     * 创建用户
     *
     * @param request 创建用户请求
     * @return 创建结果
     */
    public UserCreateResp createUser(UserCreateReq request) {
        return remoteCallExecutor.execute(RemoteHttpConstant.CLIENT_DEFAULT, () -> restClient.post()
                .uri("/api/users")
                .body(request)
                .retrieve()
                .body(UserCreateResp.class));
    }

    public record UserDetailResp(Long id, String username, String nickname) {
    }

    public record UserCreateReq(String username, String nickname) {
    }

    public record UserCreateResp(Long id, Boolean success) {
    }

}