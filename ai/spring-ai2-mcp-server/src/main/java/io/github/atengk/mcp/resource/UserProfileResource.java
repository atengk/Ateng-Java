package io.github.atengk.mcp.resource;

import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Resource：用户资料资源服务
 * <p>
 * 该组件用于根据用户名返回结构化的用户资料信息，
 * 适合演示 JSON 资源返回方式。
 *
 * @author Ateng
 * @since 2026-04-23
 */
@Service
public class UserProfileResource {

    /**
     * 日志组件
     */
    private static final Logger log = LoggerFactory.getLogger(UserProfileResource.class);

    /**
     * 根据用户名读取用户资料
     *
     * @param username 用户名
     * @return 用户资料资源结果
     */
    @McpResource(
            uri = "user-profile://{username}",
            name = "userProfileResource",
            title = "用户资料资源",
            description = "根据用户名返回结构化的用户资料信息",
            mimeType = "application/json"
    )
    public ReadResourceResult getUserProfile(String username) {
        log.debug("MCP资源[userProfileResource]被访问，username={}", username);

        Map<String, Object> profile = Map.of(
                "username", username,
                "nickname", username + "_nick",
                "age", 26,
                "city", "杭州",
                "status", "ACTIVE"
        );

        String content = JSONUtil.toJsonStr(profile);

        log.debug("MCP资源[userProfileResource]返回成功，username={}", username);
        return new ReadResourceResult(List.of(
                new TextResourceContents(
                        "user-profile://" + username,
                        "application/json",
                        content
                )
        ));
    }
}