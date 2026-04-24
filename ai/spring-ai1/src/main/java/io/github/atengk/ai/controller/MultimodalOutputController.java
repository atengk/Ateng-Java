package io.github.atengk.ai.controller;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 多模态输出控制器
 *
 * @author Ateng
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/api/ai")
public class MultimodalOutputController {

    private final ImageModel imageModel;
    private final TranscriptionModel transcriptionModel;
    private final TextToSpeechModel textToSpeechModel;

    public MultimodalOutputController(
            ImageModel imageModel,
            TranscriptionModel transcriptionModel,
            TextToSpeechModel textToSpeechModel) {
        this.imageModel = imageModel;
        this.transcriptionModel = transcriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    /**
     * 最基础的图片生成
     */
    @GetMapping("/image/generate")
    public String generateImage(@RequestParam String message) {
        ImageResponse response = imageModel.call(new ImagePrompt(message));
        return response.getResult().getOutput().getUrl();
    }

    /**
     * 指定参数生成图片
     */
    @GetMapping("/image/generate/options")
    public String generateImageWithOptions(@RequestParam String message) {
        ImageResponse response = imageModel.call(
                new ImagePrompt(
                        message,
                        OpenAiImageOptions.builder()
                                .model("dall-e-3")
                                .width(1024)
                                .height(1024)
                                .quality("hd")
                                .style("vivid")
                                .build()
                )
        );
        return response.getResult().getOutput().getUrl();
    }


    /**
     * 返回 Base64 图片内容
     */
    @GetMapping("/image/generate/base64")
    public String generateImageBase64(@RequestParam String message) {
        ImageResponse response = imageModel.call(
                new ImagePrompt(
                        message,
                        OpenAiImageOptions.builder()
                                .model("dall-e-3")
                                .responseFormat("b64_json")
                                .build()
                )
        );
        return response.getResult().getOutput().getB64Json();
    }

    /**
     * 最基础的语音转文字
     */
    @PostMapping(value = "/audio/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribe(@RequestParam MultipartFile file) throws Exception {
        ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        return transcriptionModel.transcribe(audioResource);
    }

    /**
     * 带参数的语音转文字
     */
    @PostMapping(value = "/audio/transcribe/options", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String transcribeWithOptions(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "以下内容主要是Java和Spring AI相关技术分享") String prompt) throws Exception {

        ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .language("zh")
                .prompt(prompt)
                .temperature(0f)
                .build();

        return transcriptionModel.transcribe(audioResource, options);
    }

    /**
     * 最基础的文本转语音
     */
    @GetMapping("/audio/speech")
    public ResponseEntity<byte[]> textToSpeech(@RequestParam String message) {
        byte[] audio = textToSpeechModel.call(message);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=speech.mp3")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }

    /**
     * 自定义语音参数的文本转语音
     */
    @GetMapping("/audio/speech/options")
    public ResponseEntity<byte[]> textToSpeechWithOptions(@RequestParam String message) {
        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .model("gpt-4o-mini-tts")
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .speed(1.0)
                .build();

        byte[] audio = textToSpeechModel
                .call(new TextToSpeechPrompt(message, options))
                .getResult()
                .getOutput();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=speech-custom.mp3")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }




}