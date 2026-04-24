package io.github.atengk.ai.controller;


import cn.hutool.core.util.StrUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 多模态对话控制器
 *
 * @author Ateng
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/ai")
public class MultimodalChatController {

    private final ChatClient chatClient;

    public MultimodalChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping(value = "/chat/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String chatWithImage(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "请描述这张图片的内容") String message) throws Exception {

        String contentType = StrUtil.blankToDefault(file.getContentType(), MediaType.IMAGE_PNG_VALUE);
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        return chatClient
                .prompt()
                .user(u -> u
                        .text(message)
                        .media(mimeType, imageResource)
                )
                .call()
                .content();
    }

    @PostMapping(value = "/chat/image/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String analysisImage(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "请识别图片内容，并使用Markdown格式输出") String message) throws Exception {

        String contentType = StrUtil.blankToDefault(file.getContentType(), MediaType.IMAGE_PNG_VALUE);
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        ByteArrayResource imageResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        return chatClient
                .prompt()
                .user(u -> u
                        .text("""
                            你是一个专业的图片分析助手。
                            请根据用户上传的图片完成分析任务。

                            用户要求：
                            {message}
                            """)
                        .param("message", message)
                        .media(mimeType, imageResource)
                )
                .call()
                .content();
    }

    @PostMapping(value = "/chat/images/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String compareImages(
            @RequestParam MultipartFile firstFile,
            @RequestParam MultipartFile secondFile,
            @RequestParam(defaultValue = "请对比两张图片的主要差异") String message) throws Exception {

        String firstContentType = StrUtil.blankToDefault(firstFile.getContentType(), MediaType.IMAGE_PNG_VALUE);
        String secondContentType = StrUtil.blankToDefault(secondFile.getContentType(), MediaType.IMAGE_PNG_VALUE);

        ByteArrayResource firstResource = new ByteArrayResource(firstFile.getBytes()) {
            @Override
            public String getFilename() {
                return firstFile.getOriginalFilename();
            }
        };

        ByteArrayResource secondResource = new ByteArrayResource(secondFile.getBytes()) {
            @Override
            public String getFilename() {
                return secondFile.getOriginalFilename();
            }
        };

        return chatClient
                .prompt()
                .user(u -> u
                        .text(message)
                        .media(MimeTypeUtils.parseMimeType(firstContentType), firstResource)
                        .media(MimeTypeUtils.parseMimeType(secondContentType), secondResource)
                )
                .call()
                .content();
    }

    @PostMapping(value = "/chat/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String chatWithAudio(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "请总结这段音频的主要内容") String message) throws Exception {

        String contentType = StrUtil.blankToDefault(file.getContentType(), "audio/mpeg");
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        return chatClient
                .prompt()
                .user(u -> u
                        .text(message)
                        .media(mimeType, audioResource)
                )
                .call()
                .content();
    }

    @PostMapping(value = "/chat/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String chatWithFile(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "请总结这个文件的核心内容") String message) throws Exception {

        String contentType = StrUtil.blankToDefault(file.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        return chatClient
                .prompt()
                .user(u -> u
                        .text(message)
                        .media(mimeType, fileResource)
                )
                .call()
                .content();
    }

    @GetMapping("/chat/image/local")
    public String chatWithLocalImage(
            @RequestParam(defaultValue = "请描述这张图片的内容") String message) {

        return chatClient
                .prompt()
                .user(u -> u
                        .text(message)
                        .media(MimeTypeUtils.IMAGE_PNG, new org.springframework.core.io.ClassPathResource("/images/test.png"))
                )
                .call()
                .content();
    }


}
