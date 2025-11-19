package local.ateng.java.mybatisjdk8;


import local.ateng.java.customutils.utils.ImageUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * ImageUtil 工具类的单元测试
 * 主要验证图片的读取、写出、Base64 转换等基础功能是否正常工作
 *
 * @author Ateng
 * @since 2025-11-20
 */
public class ImageUtilTests {

    /**
     * 测试资源图片路径
     */
    private static final String TEST_IMAGE_PATH = "D:\\Temp\\202511\\background_anima.png";
    private static final String TEST_IMAGE_PATH_JPG = "D:\\Temp\\202511\\mushrooms-9494682_1280.jpg";

    /**
     * 测试输出文件路径
     */
    private static final String OUTPUT_IMAGE_PATH = "target/test-output.png";
    private static final String OUTPUT_DIR = "target/test-output/";


    /**
     * 默认格式
     */
    private static final String IMAGE_FORMAT = "png";
    private static final String FORMAT_JPG = "jpg";
    private static final String FORMAT_PNG = "png";

    /**
     * 测试从文件读取图片
     *
     * @throws Exception IO异常
     */
    @Test
    public void testReadImageFromFile() throws Exception {
        File file = new File(TEST_IMAGE_PATH);
        BufferedImage image = ImageUtil.readImage(file);
        Assertions.assertNotNull(image);
        Assertions.assertTrue(image.getWidth() > 0);
        Assertions.assertTrue(image.getHeight() > 0);
    }

    /**
     * 测试从输入流读取图片
     *
     * @throws Exception IO异常
     */
    @Test
    public void testReadImageFromInputStream() throws Exception {
        FileInputStream inputStream = new FileInputStream(TEST_IMAGE_PATH);
        BufferedImage image = ImageUtil.readImage(inputStream);
        Assertions.assertNotNull(image);
        Assertions.assertTrue(image.getWidth() > 0);
    }

    /**
     * 测试从 URL 读取图片
     *
     * @throws Exception IO异常
     */
    @Test
    public void testReadImageFromUrl() throws Exception {
        URL url = new URL("https://lokeshdhakar.com/projects/lightbox2/images/thumb-4.jpg");
        BufferedImage image = ImageUtil.readImage(url);
        Assertions.assertNotNull(image);
        Assertions.assertTrue(image.getWidth() > 0);
    }

    /**
     * 测试将图片写出到文件
     *
     * @throws Exception IO异常
     */
    @Test
    public void testWriteImageToFile() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        File output = new File(OUTPUT_IMAGE_PATH);
        ImageUtil.writeImageToFile(image, IMAGE_FORMAT, output);
        Assertions.assertTrue(output.exists());
        Assertions.assertTrue(output.length() > 0);
    }

    /**
     * 测试写出为字节数组
     *
     * @throws Exception IO异常
     */
    @Test
    public void testWriteImageToBytes() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        byte[] bytes = ImageUtil.writeImageToBytes(image, IMAGE_FORMAT);
        Assertions.assertNotNull(bytes);
        Assertions.assertTrue(bytes.length > 0);
    }

    /**
     * 测试图片转 Base64
     *
     * @throws Exception IO异常
     */
    @Test
    public void testWriteImageToBase64() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        String base64 = ImageUtil.writeImageToBase64(image, IMAGE_FORMAT);
        Assertions.assertNotNull(base64);
        Assertions.assertFalse(base64.isEmpty());
    }

    /**
     * 测试 Base64 转图片
     *
     * @throws Exception IO异常
     */
    @Test
    public void testReadImageFromBase64() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        String base64 = ImageUtil.writeImageToBase64(image, IMAGE_FORMAT);
        BufferedImage result = ImageUtil.readImageFromBase64(base64);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(image.getWidth(), result.getWidth());
    }


    /**
     * 测试 PNG 转 JPG
     *
     * @throws Exception IO异常
     */
    @Test
    public void testPngToJpg() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        BufferedImage result = ImageUtil.pngToJpg(image);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(image.getWidth(), result.getWidth());
        Assertions.assertEquals(image.getHeight(), result.getHeight());
        Assertions.assertEquals(BufferedImage.TYPE_INT_RGB, result.getType());
    }

    /**
     * 测试 JPG 转 PNG
     *
     * @throws Exception IO异常
     */
    @Test
    public void testJpgToPng() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH_JPG));
        BufferedImage result = ImageUtil.jpgToPng(image);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(image.getWidth(), result.getWidth());
        Assertions.assertEquals(image.getHeight(), result.getHeight());
    }

    /**
     * 测试转换为指定格式
     *
     * @throws Exception IO异常
     */
    @Test
    public void testConvertFormat() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        BufferedImage result = ImageUtil.convertFormat(image, FORMAT_JPG);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(image.getWidth(), result.getWidth());
        Assertions.assertEquals(image.getHeight(), result.getHeight());
    }

    /**
     * 测试按比例缩放
     *
     * @throws Exception IO异常
     */
    @Test
    public void testScaleByRatio() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        double scale = 0.5;

        BufferedImage result = ImageUtil.scaleByRatio(image, scale);

        ImageUtil.writeImageToFile(result, "png", new File("D:\\Temp\\202511\\1.png"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals((int) (image.getWidth() * scale), result.getWidth());
        Assertions.assertEquals((int) (image.getHeight() * scale), result.getHeight());
    }

    /**
     * 测试指定宽高缩放（不等比）
     *
     * @throws Exception IO异常
     */
    @Test
    public void testScale() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        int targetWidth = 200;
        int targetHeight = 120;

        BufferedImage result = ImageUtil.scale(image, targetWidth, targetHeight);

        ImageUtil.writeImageToFile(result, "png", new File("D:\\Temp\\202511\\2.png"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(targetWidth, result.getWidth());
        Assertions.assertEquals(targetHeight, result.getHeight());
    }

    /**
     * 测试限制最大宽高等比缩放
     *
     * @throws Exception IO异常
     */
    @Test
    public void testScaleLimit() throws Exception {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        int maxWidth = 100;
        int maxHeight = 100;

        BufferedImage result = ImageUtil.scaleLimit(image, maxWidth, maxHeight);

        ImageUtil.writeImageToFile(result, "png", new File("D:\\Temp\\202511\\3.png"));

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getWidth() <= maxWidth);
        Assertions.assertTrue(result.getHeight() <= maxHeight);
    }


    /**
     * 测试图片按坐标裁剪
     */
    @Test
    public void testCropByRegion() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage cropped = ImageUtil.crop(image, 1000, 1000, 500, 500);
        Assertions.assertNotNull(cropped);

        ImageUtil.writeImageToFile(cropped, "jpg", new File(OUTPUT_DIR + "crop-region.jpg"));

    }

    /**
     * 测试中心裁剪
     */
    @Test
    public void testCropCenter() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage cropped = ImageUtil.cropCenter(image, 1000, 1000);
        Assertions.assertNotNull(cropped);

        ImageUtil.writeImageToFile(cropped, "jpg", new File(OUTPUT_DIR + "crop-center.jpg"));
    }

    /**
     * 测试自动裁剪为正方形
     */
    @Test
    public void testCropSquare() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage square = ImageUtil.cropSquare(image);
        Assertions.assertNotNull(square);
        Assertions.assertEquals(square.getWidth(), square.getHeight());

        ImageUtil.writeImageToFile(square, "jpg", new File(OUTPUT_DIR + "crop-square.jpg"));
    }

    /**
     * 测试旋转任意角度
     */
    @Test
    public void testRotate() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage rotated = ImageUtil.rotate(image, 45);
        Assertions.assertNotNull(rotated);

        ImageUtil.writeImageToFile(rotated, "jpg", new File(OUTPUT_DIR + "rotate-45.jpg"));
    }

    /**
     * 测试水平翻转
     */
    @Test
    public void testFlipHorizontal() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage flipped = ImageUtil.flipHorizontal(image);
        Assertions.assertNotNull(flipped);

        ImageUtil.writeImageToFile(flipped, "jpg", new File(OUTPUT_DIR + "flip-horizontal.jpg"));
    }

    /**
     * 测试垂直翻转
     */
    @Test
    public void testFlipVertical() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage flipped = ImageUtil.flipVertical(image);
        Assertions.assertNotNull(flipped);

        ImageUtil.writeImageToFile(flipped, "jpg", new File(OUTPUT_DIR + "flip-vertical.jpg"));
    }

    /**
     * 测试自动裁剪为圆形（头像常用）
     */
    @Test
    public void testCropCircleAuto() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage circle = ImageUtil.cropCircle(image);
        Assertions.assertNotNull(circle);
        Assertions.assertEquals(circle.getWidth(), circle.getHeight());

        ImageUtil.writeImageToFile(circle, "png", new File(OUTPUT_DIR + "circle-auto.png"));
    }

    /**
     * 测试指定圆心与半径裁剪
     */
    @Test
    public void testCropCircleByCenter() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        int radius = 100;
        int centerX = image.getWidth() / 2;
        int centerY = image.getHeight() / 2;

        BufferedImage circle = ImageUtil.cropCircle(image, centerX, centerY, radius);
        Assertions.assertNotNull(circle);
        Assertions.assertEquals(radius * 2, circle.getWidth());
        Assertions.assertEquals(radius * 2, circle.getHeight());

        ImageUtil.writeImageToFile(circle, "png", new File(OUTPUT_DIR + "circle-center.png"));
    }

    /**
     * 测试指定直径裁剪为圆形（头像特定尺寸）
     */
    @Test
    public void testCropCircleByDiameter() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        int diameter = 256;

        BufferedImage circle = ImageUtil.cropCircle(image, diameter);
        Assertions.assertNotNull(circle);
        Assertions.assertEquals(256, circle.getWidth());
        Assertions.assertEquals(256, circle.getHeight());

        ImageUtil.writeImageToFile(circle, "png", new File(OUTPUT_DIR + "circle-diameter.png"));
    }

    /**
     * 测试 JPEG 按质量压缩
     */
    @Test
    public void testCompressByQuality() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        byte[] compressed = ImageUtil.compressJpeg(image, 0.5f);

        Assertions.assertNotNull(compressed);
        Assertions.assertTrue(compressed.length > 0);

        Files.write(Paths.get(OUTPUT_DIR + "compress-quality.jpg"), compressed);
    }

    /**
     * 测试无尺寸变化压缩
     */
    @Test
    public void testCompressKeepSize() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        byte[] compressed = ImageUtil.compressKeepSize(image, 300 * 1024);

        Assertions.assertNotNull(compressed);
        Assertions.assertTrue(compressed.length <= 300 * 1024);

        Files.write(Paths.get(OUTPUT_DIR + "compress-keep-size.jpg"), compressed);
    }

    /**
     * 测试智能压缩（SmartCompress）
     */
    @Test
    public void testSmartCompress() throws IOException {
        // 读取测试图片
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        // 设置智能压缩参数
        int maxWidth = 1024;          // 最大宽度
        int maxHeight = 1024;         // 最大高度
        long targetSize = 400 * 1024; // 目标大小 400KB
        float minQuality = 0.5f;      // 最小压缩质量

        // 调用智能压缩
        byte[] compressed = ImageUtil.smartCompress(image, maxWidth, maxHeight, targetSize, minQuality);

        // 断言结果不为空
        Assertions.assertNotNull(compressed);

        // 断言压缩后大小小于等于目标大小
        Assertions.assertTrue(compressed.length <= targetSize, "压缩后的大小超出目标大小");

        // 将压缩后的图片写出到输出目录
        Files.write(Paths.get(OUTPUT_DIR + "compress-smart.jpg"), compressed);
    }

    /**
     * 测试添加文字水印
     */
    @Test
    public void testAddTextWatermark() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        BufferedImage result = ImageUtil.addTextWatermark(
                image,
                "Ateng 版权所有",
                "宋体",
                24,
                0.6f,
                ImageUtil.Position.BOTTOM_RIGHT
        );

        Assertions.assertNotNull(result);
        ImageUtil.writeImageToFile(result, "png", new File(OUTPUT_DIR + "watermark-text.png"));
    }

    /**
     * 测试添加图片水印
     */
    @Test
    public void testAddImageWatermark() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));
        BufferedImage logo = ImageUtil.readImage(new File("D:\\Temp\\202511\\通知.jpeg"));

        BufferedImage result = ImageUtil.addImageWatermark(
                image,
                logo,
                0.5f,
                ImageUtil.Position.CENTER
        );

        Assertions.assertNotNull(result);
        ImageUtil.writeImageToFile(result, "png", new File(OUTPUT_DIR + "watermark-image.png"));
    }

    /**
     * 测试获取宽度
     */
    @Test
    public void testGetWidth() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        int width = ImageUtil.getWidth(image);
        Assertions.assertTrue(width > 0);
    }

    /**
     * 测试获取高度
     */
    @Test
    public void testGetHeight() throws IOException {
        BufferedImage image = ImageUtil.readImage(new File(TEST_IMAGE_PATH));

        int height = ImageUtil.getHeight(image);
        Assertions.assertTrue(height > 0);
    }

    /**
     * 测试获取格式
     */
    @Test
    public void testGetFormat() throws IOException {
        String format = ImageUtil.getFormat(TEST_IMAGE_PATH);

        Assertions.assertNotNull(format);
        Assertions.assertTrue(format.equalsIgnoreCase("jpg")
                || format.equalsIgnoreCase("jpeg")
                || format.equalsIgnoreCase("png"));
    }

    /**
     * 测试获取文件大小
     */
    @Test
    public void testGetFileSize() throws IOException {
        long size = ImageUtil.getFileSize(TEST_IMAGE_PATH);

        Assertions.assertTrue(size > 0);
    }

}