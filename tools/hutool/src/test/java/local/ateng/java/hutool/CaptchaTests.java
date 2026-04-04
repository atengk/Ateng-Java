package local.ateng.java.hutool;

import cn.hutool.captcha.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 * 验证码工具测试（完整用法集合）
 */
public class CaptchaTests {

    /**
     * 获取 target 目录
     */
    private String getTargetPath(String fileName) {
        return System.getProperty("user.dir") + File.separator + "target" + File.separator + fileName;
    }

    /**
     * 1. 线段干扰验证码（最常用）
     */
    @Test
    public void lineCaptcha() throws IOException {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 100, 5, 20);

        String path = getTargetPath("line.png");
        captcha.write(path);

        System.out.println("Line验证码：" + captcha.getCode());
    }

    /**
     * 2. 圆圈干扰验证码
     */
    @Test
    public void circleCaptcha() throws IOException {
        CircleCaptcha captcha = CaptchaUtil.createCircleCaptcha(200, 100, 5, 20);

        String path = getTargetPath("circle.png");
        captcha.write(path);

        System.out.println("Circle验证码：" + captcha.getCode());
    }

    /**
     * 3. 扭曲干扰验证码（更复杂）
     */
    @Test
    public void shearCaptcha() throws IOException {
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(200, 100, 5, 4);

        String path = getTargetPath("shear.png");
        captcha.write(path);

        System.out.println("Shear验证码：" + captcha.getCode());
    }

    /**
     * 4. GIF 动态验证码（推荐登录用）
     */
    @Test
    public void gifCaptcha() throws IOException {
        GifCaptcha captcha = CaptchaUtil.createGifCaptcha(200, 100, 5);

        String path = getTargetPath("gif.gif");
        captcha.write(path);

        System.out.println("GIF验证码：" + captcha.getCode());
    }

    /**
     * 生成 Base64 验证码
     */
    @Test
    public void generateBase64Captcha() {

        /*
         * 创建验证码
         * width: 宽度
         * height: 高度
         * codeCount: 字符数量
         * lineCount: 干扰线数量
         */
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 100, 5, 20);

        /*
         * 获取 Base64 编码（不带 data:image 前缀）
         */
        String base64 = captcha.getImageBase64();

        /*
         * 获取验证码内容（用于后端校验）
         */
        String code = captcha.getCode();

        /*
         * 输出结果（仅测试用）
         */
        System.out.println("验证码：" + code);
        System.out.println("Base64：" + base64);

        // 前端使用方式
        // <img :src="'data:image/png;base64,' + base64" />
    }

}