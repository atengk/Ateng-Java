package io.github.atengk.onlyoffice.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.jwt.JWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * OnlyOffice 在线编辑集成控制器
 * <p>
 * 作用：
 * 1. 提供前端 DocsAPI.DocEditor 所需的配置（/config）
 * 2. 接收 OnlyOffice DocumentServer 的回调（/callback）
 * <p>
 * 适配版本：
 * onlyoffice/documentserver:9.2
 */
@Slf4j
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/onlyoffice")
public class OnlyOfficeController {

    /* ========================= 常量配置区 ========================= */

    /**
     * OnlyOffice JWT 密钥
     * ⚠️ 必须与 DocumentServer 环境变量 JWT_SECRET 保持一致
     */
    private static final String ONLYOFFICE_JWT_SECRET = "Admin@123";

    /**
     * 文件访问基础地址（文件必须是 DocumentServer 可直接访问的公网地址）
     * 示例：http://ip:port/data/demo.docx
     */
    private static final String FILE_BASE_URL = "http://47.108.128.105:20006/data/";

    /**
     * OnlyOffice 回调地址
     * status=2 / status=6 时会携带文件下载地址
     */
    private static final String CALLBACK_URL = "http://47.108.128.105:11013/onlyoffice/callback";

    /**
     * 编辑模式：edit / view
     */
    private static final String EDIT_MODE = "edit";

    /**
     * 当前演示用户 ID
     * 实际项目中应来自登录用户
     */
    private static final String DEMO_USER_ID = "1";

    /**
     * 当前演示用户名称
     */
    private static final String DEMO_USER_NAME = "测试用户";

    /* ========================= 接口区 ========================= */

    /**
     * 获取 OnlyOffice 编辑器配置
     *
     * @param fileName 文件名（如 demo.docx / demo.xlsx）
     * @param fileType 文件类型（docx / xlsx / pptx）
     * @return OnlyOffice DocEditor 初始化配置
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig(@RequestParam String fileName,
                                         @RequestParam String fileType,
                                         @RequestParam(defaultValue = "false") Boolean preview) {

        /* ---------- 1. document 配置 ---------- */

        Map<String, Object> document = new HashMap<>();
        document.put("fileType", fileType);
        //document.put("key", DigestUtil.md5Hex(fileName)); // 文件唯一标识（同一文件必须固定），注意不要有中文
        document.put("key", IdUtil.fastSimpleUUID()); // 测试用
        document.put("title", fileName);
        document.put("url", FILE_BASE_URL + fileName);

        /* ---------- 2. editorConfig 配置 ---------- */

        Map<String, Object> editorConfig = new HashMap<>();
        /**
         * edit  : 编辑模式
         * view  : 预览（只读）模式
         */
        editorConfig.put("mode", preview ? "view" : "edit");
        /**
         * 语言
         */
        editorConfig.put("lang", "zh-CN");

        Map<String, Object> user = new HashMap<>();
        user.put("id", DEMO_USER_ID);
        user.put("name", DEMO_USER_NAME);
        editorConfig.put("user", user);

        editorConfig.put("callbackUrl", CALLBACK_URL);

        /* ---------- 3. 顶层 config ---------- */

        Map<String, Object> config = new HashMap<>();
        config.put("document", document);
        config.put("editorConfig", editorConfig);

        /* ---------- 4. 生成 JWT（基于最终 config） ---------- */

        JWT jwt = JWT.create();
        jwt.setPayload("document", document);
        jwt.setPayload("editorConfig", editorConfig);

        String token = jwt
                .setKey(ONLYOFFICE_JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                .sign();

        config.put("token", token);

        return config;
    }

    /**
     * OnlyOffice 回调接口
     * <p>
     * 回调触发说明：
     * status=1  打开文档
     * status=2  关闭文档并保存（最重要）
     * status=6  强制保存完成
     *
     * @param body DocumentServer 回调数据
     * @return error=0 表示接收成功
     */
    @PostMapping("/callback")
    public Map<String, Object> callback(@RequestBody Map<String, Object> body) {

        log.info("OnlyOffice callback: {}", body);

        Integer status = (Integer) body.get("status");

        // status=2 或 status=6 才是真正需要保存文件的时机
        if (status != null && (status == 2 || status == 6)) {
            String fileUrl = (String) body.get("url");
            String fileKey = (String) body.get("key");

            log.info("Need save file: key={}, url={}", fileKey, fileUrl);

            // TODO:
            // 1. 下载 fileUrl
            // 2. 覆盖原文件（本地 / MinIO / OSS）
        }

        Map<String, Object> result = new HashMap<>();
        result.put("error", 0);
        return result;
    }
}
