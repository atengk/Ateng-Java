package local.ateng.java.customutils.utils;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Iterator;

/**
 * 图片工具类
 * 提供图片的读取、写出、格式转换等基础功能
 *
 * @author Ateng
 * @since 2025-11-20
 */
public final class ImageUtil {

    /**
     * 默认图片格式
     */
    private static final String DEFAULT_IMAGE_FORMAT = "png";


    /**
     * JPEG 格式
     */
    public static final String JPG = "jpg";

    /**
     * JPEG 格式（备用）
     */
    public static final String JPEG = "jpeg";

    /**
     * PNG 格式
     */
    public static final String PNG = "png";

    /**
     * GIF 格式
     */
    public static final String GIF = "gif";

    /**
     * BMP 格式
     */
    public static final String BMP = "bmp";

    /**
     * WBMP 格式
     */
    public static final String WBMP = "wbmp";

    /**
     * TIFF 格式
     */
    public static final String TIFF = "tiff";

    /**
     * TIF 格式
     */
    public static final String TIF = "tif";

    /**
     * 水印位置枚举
     */
    public enum Position {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }

    /**
     * 工具类私有构造方法
     */
    private ImageUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 从文件路径读取图片
     *
     * @param file 文件对象
     * @return 读取到的 BufferedImage
     * @throws IOException IO异常
     */
    public static BufferedImage readImage(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("文件不存在");
        }
        return ImageIO.read(file);
    }

    /**
     * 从输入流读取图片
     *
     * @param inputStream 输入流
     * @return 读取到的 BufferedImage
     * @throws IOException IO异常
     */
    public static BufferedImage readImage(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不可为 null");
        }
        return ImageIO.read(inputStream);
    }

    /**
     * 从网络 URL 读取图片
     *
     * @param url 图片的 URL
     * @return 读取到的 BufferedImage
     * @throws IOException IO异常
     */
    public static BufferedImage readImage(URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL 不可为 null");
        }
        return ImageIO.read(url);
    }

    /**
     * 从路径或 URL 读取图片
     * <p>
     * 自动判断：
     * 1. 本地文件路径 → 读取 File
     * 2. HTTP/HTTPS URL → 读取网络图片
     * 3. 其他 URL（file://、ftp:// 等） → 自动识别
     *
     * @param pathOrUrl 图片路径或 URL
     * @return 读取到的 BufferedImage
     * @throws IOException IO异常
     */
    public static BufferedImage readImage(String pathOrUrl) throws IOException {
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("路径或 URL 不能为空");
        }

        pathOrUrl = pathOrUrl.trim();

        // 判断是否为 URL
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            try (InputStream input = new URL(pathOrUrl).openStream()) {
                return ImageIO.read(input);
            }
        }

        // 判断是否为 file:// URL
        if (pathOrUrl.startsWith("file://")) {
            try {
                URL fileUrl = new URL(pathOrUrl);
                return ImageIO.read(fileUrl);
            } catch (MalformedURLException e) {
                throw new IOException("无效的文件 URL", e);
            }
        }
        // 判断是否为 ftp:// URL
        else if (pathOrUrl.startsWith("ftp://")) {
            try {
                URL fileUrl = new URL(pathOrUrl);
                return ImageIO.read(fileUrl);
            } catch (MalformedURLException e) {
                throw new IOException("无效的文件 URL", e);
            }
        }

        // 默认尝试本地文件路径
        File file = new File(pathOrUrl);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在：" + pathOrUrl);
        }

        return ImageIO.read(file);
    }

    /**
     * 将图片写出到文件
     *
     * @param image  图片对象
     * @param format 图片格式，例如 png、jpg
     * @param file   输出文件
     * @throws IOException IO异常
     */
    public static void writeImageToFile(BufferedImage image, String format, File file) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("图片对象不可为 null");
        }
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("图片格式不可为空");
        }
        if (file == null) {
            throw new IllegalArgumentException("输出文件不可为 null");
        }
        ImageIO.write(image, format, file);
    }

    /**
     * 将图片输出为 byte 数组
     *
     * @param image  图片对象
     * @param format 输出格式
     * @return 图片的 byte 数组
     * @throws IOException IO异常
     */
    public static byte[] writeImageToBytes(BufferedImage image, String format) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("图片对象不可为 null");
        }
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("输出格式不可为空");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 将图片转换为 Base64 字符串
     *
     * @param image  图片对象
     * @param format 输出格式
     * @return Base64 字符串
     * @throws IOException IO异常
     */
    public static String writeImageToBase64(BufferedImage image, String format) throws IOException {
        byte[] bytes = writeImageToBytes(image, format);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 将 Base64 字符串转回 BufferedImage
     *
     * @param base64 Base64 字符串
     * @return 图片对象
     * @throws IOException IO异常
     */
    public static BufferedImage readImageFromBase64(String base64) throws IOException {
        if (base64 == null || base64.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 字符串不可为空");
        }
        byte[] bytes = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return ImageIO.read(inputStream);
    }

    /**
     * 获取默认图片格式
     *
     * @return 默认格式
     */
    public static String getDefaultImageFormat() {
        return DEFAULT_IMAGE_FORMAT;
    }

    /**
     * ================================
     * 一、图片格式转换
     * ================================
     */

    /**
     * 将图片转换为指定格式并返回 BufferedImage
     *
     * @param image        源图片
     * @param targetFormat 目标格式，例如 jpg、png
     * @return 转换后的 BufferedImage
     */
    public static BufferedImage convertFormat(BufferedImage image, String targetFormat) {
        if (image == null) {
            throw new IllegalArgumentException("源图片不可为 null");
        }
        if (targetFormat == null || targetFormat.trim().isEmpty()) {
            throw new IllegalArgumentException("目标格式不可为空");
        }
        BufferedImage newImage;

        if ("jpg".equalsIgnoreCase(targetFormat) || "jpeg".equalsIgnoreCase(targetFormat)) {
            newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            newImage.getGraphics().drawImage(image, 0, 0, null);
        } else {
            newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            newImage.getGraphics().drawImage(image, 0, 0, null);
        }
        return newImage;
    }

    /**
     * PNG 转 JPG
     *
     * @param image PNG 图片
     * @return JPG 格式图片
     */
    public static BufferedImage pngToJpg(BufferedImage image) {
        return convertFormat(image, "jpg");
    }

    /**
     * JPG 转 PNG
     *
     * @param image JPG 图片
     * @return PNG 格式图片
     */
    public static BufferedImage jpgToPng(BufferedImage image) {
        return convertFormat(image, "png");
    }


/**
 * ================================
 * 二、图片缩放（等比、不等比）
 * ================================
 */

    /**
     * 按比例缩放图片
     *
     * @param image 源图片
     * @param scale 缩放比例，例如 0.5 表示缩小一半
     * @return 缩放后的图片
     */
    public static BufferedImage scaleByRatio(BufferedImage image, double scale) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("缩放比例必须大于 0");
        }
        int targetWidth = (int) (image.getWidth() * scale);
        int targetHeight = (int) (image.getHeight() * scale);

        return scale(image, targetWidth, targetHeight);
    }

    /**
     * 按指定宽高缩放（不等比）
     *
     * @param image        源图片
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @return 缩放后的图片
     */
    public static BufferedImage scale(BufferedImage image, int targetWidth, int targetHeight) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("目标宽高必须大于 0");
        }

        BufferedImage newImage = new BufferedImage(targetWidth, targetHeight, image.getType());
        newImage.getGraphics().drawImage(image, 0, 0, targetWidth, targetHeight, null);
        return newImage;
    }

    /**
     * 限制最大宽高等比缩放（常用于上传头像）
     *
     * @param image     源图片
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 缩放后的图片
     */
    public static BufferedImage scaleLimit(BufferedImage image, int maxWidth, int maxHeight) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("最大宽高必须大于 0");
        }

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return image;
        }

        double scaleWidth = (double) maxWidth / originalWidth;
        double scaleHeight = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleWidth, scaleHeight);

        int targetWidth = (int) (originalWidth * scale);
        int targetHeight = (int) (originalHeight * scale);

        return scale(image, targetWidth, targetHeight);
    }

    /**
     * ================================
     * 五、图片裁剪
     * ================================
     */

    /**
     * 按指定坐标与宽高裁剪图片
     *
     * @param image  源图片
     * @param x      裁剪起点 X 坐标
     * @param y      裁剪起点 Y 坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    public static BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("裁剪宽高必须大于 0");
        }
        if (x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IllegalArgumentException("裁剪区域超出图片范围");
        }

        return image.getSubimage(x, y, width, height);
    }

    /**
     * 从图片中心裁剪指定宽高区域
     *
     * @param image  源图片
     * @param width  目标宽度
     * @param height 目标高度
     * @return 中心裁剪后的图片
     */
    public static BufferedImage cropCenter(BufferedImage image, int width, int height) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("裁剪宽高必须大于 0");
        }
        if (width > image.getWidth() || height > image.getHeight()) {
            throw new IllegalArgumentException("裁剪尺寸不能大于原图");
        }

        int x = (image.getWidth() - width) / 2;
        int y = (image.getHeight() - height) / 2;

        return crop(image, x, y, width, height);
    }

    /**
     * 自动裁剪为正方形（头像处理常用）
     * 从中心区域裁剪为边长为 min(width, height) 的正方形
     *
     * @param image 源图片
     * @return 正方形裁剪图
     */
    public static BufferedImage cropSquare(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }

        int side = Math.min(image.getWidth(), image.getHeight());
        return cropCenter(image, side, side);
    }


    /**
     * 自动裁剪为圆形头像。
     * 实现步骤：
     * 1. 自动裁剪为正方形（以图片中心为基准）
     * 2. 将正方形裁剪为透明背景圆形
     *
     * @param source 原始图片
     * @return 圆形图片，格式为 ARGB
     */
    public static BufferedImage cropCircle(BufferedImage source) {
        if (source == null) {
            throw new IllegalArgumentException("源图片不能为空");
        }

        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        BufferedImage square = source.getSubimage(x, y, size, size);

        return createCircleImage(square, size);
    }

    /**
     * 按指定圆心与半径裁剪为圆形图片。
     *
     * @param source  原始图片
     * @param centerX 圆心 X 坐标
     * @param centerY 圆心 Y 坐标
     * @param radius  半径
     * @return 裁剪后的圆形图片（ARGB）
     */
    public static BufferedImage cropCircle(BufferedImage source, int centerX, int centerY, int radius) {
        if (source == null) {
            throw new IllegalArgumentException("源图片不能为空");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("半径必须大于 0");
        }

        int diameter = radius * 2;
        int topLeftX = centerX - radius;
        int topLeftY = centerY - radius;

        if (topLeftX < 0 || topLeftY < 0 || topLeftX + diameter > source.getWidth()
                || topLeftY + diameter > source.getHeight()) {
            throw new IllegalArgumentException("裁剪范围超出图片边界");
        }

        BufferedImage square = source.getSubimage(topLeftX, topLeftY, diameter, diameter);
        return createCircleImage(square, diameter);
    }

    /**
     * 按指定直径裁剪为圆形图片。
     * 会自动缩放为指定尺寸，再进行圆形裁剪。
     *
     * @param source   原始图片
     * @param diameter 输出圆形直径
     * @return 圆形裁剪结果（ARGB）
     */
    public static BufferedImage cropCircle(BufferedImage source, int diameter) {
        if (source == null) {
            throw new IllegalArgumentException("源图片不能为空");
        }
        if (diameter <= 0) {
            throw new IllegalArgumentException("直径必须大于 0");
        }

        BufferedImage scaled = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, diameter, diameter, null);
        } finally {
            graphics.dispose();
        }

        return createCircleImage(scaled, diameter);
    }

    /**
     * 将正方形图片裁剪为圆形。
     *
     * @param square 正方形图片
     * @param size   尺寸（宽 = 高）
     * @return 圆形透明背景图片
     */
    private static BufferedImage createCircleImage(BufferedImage square, int size) {
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setComposite(AlphaComposite.Src);
            Ellipse2D ellipse = new Ellipse2D.Double(0, 0, size, size);

            graphics.setClip(ellipse);
            graphics.drawImage(square, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return output;
    }

/**
 * ================================
 * 六、旋转与翻转
 * ================================
 */

    /**
     * 将图片按指定角度旋转（顺时针）
     *
     * @param image 源图片
     * @param angle 旋转角度（单位：度）
     * @return 旋转后的图片
     */
    public static BufferedImage rotate(BufferedImage image, double angle) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }

        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int width = image.getWidth();
        int height = image.getHeight();

        int newWidth = (int) (width * cos + height * sin);
        int newHeight = (int) (height * cos + width * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g2d = rotated.createGraphics();

        int translateX = (newWidth - width) / 2;
        int translateY = (newHeight - height) / 2;

        g2d.translate(translateX, translateY);
        g2d.rotate(radians, width / 2d, height / 2d);
        g2d.drawRenderedImage(image, null);
        g2d.dispose();

        return rotated;
    }

    /**
     * 水平翻转（镜像反射）
     *
     * @param image 源图片
     * @return 水平翻转后的图片
     */
    public static BufferedImage flipHorizontal(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = result.createGraphics();

        g2d.drawImage(image, width, 0, -width, height, null);
        g2d.dispose();

        return result;
    }

    /**
     * 垂直翻转
     *
     * @param image 源图片
     * @return 垂直翻转后的图片
     */
    public static BufferedImage flipVertical(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片不可为 null");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = result.createGraphics();

        g2d.drawImage(image, 0, height, width, -height, null);
        g2d.dispose();

        return result;
    }


    // ==================== 1. 图片压缩 ====================

    /**
     * 按 JPEG 质量压缩图片
     *
     * @param image   源图片
     * @param quality 压缩质量（0.0~1.0）
     * @return 压缩后的 byte 数组
     * @throws IOException IO 异常
     */
    public static byte[] compressJpeg(BufferedImage image, float quality) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("源图片不能为空");
        }
        if (quality <= 0f || quality > 1f) {
            throw new IllegalArgumentException("质量必须在 0~1 之间");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("未找到 JPEG 写入器");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(mcios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * 在不改变尺寸情况下，压缩图片至目标大小（字节）
     *
     * @param image      原始图片
     * @param targetSize 目标大小（字节）
     * @return 压缩后的 byte[]
     * @throws IOException IO异常
     */
    public static byte[] compressKeepSize(BufferedImage image, long targetSize) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("图片不能为空");
        }
        if (targetSize <= 0) {
            throw new IllegalArgumentException("目标大小必须大于 0");
        }

        float quality = 1.0f;
        byte[] result = compressJpeg(image, quality);

        while (result.length > targetSize && quality > 0.05f) {
            quality -= 0.05f;
            result = compressJpeg(image, quality);
        }

        return result;
    }

    /**
     * 智能压缩图片（自动缩放 + 压缩质量控制）
     *
     * @param image         原始图片
     * @param maxWidth      最大宽度（超过则缩小）
     * @param maxHeight     最大高度（超过则缩小）
     * @param targetSize    目标字节大小
     * @param minQuality    最小 JPEG 质量（0~1）
     * @return 压缩后的 byte[]
     * @throws IOException IO异常
     */
    public static byte[] smartCompress(BufferedImage image, int maxWidth, int maxHeight, long targetSize, float minQuality) throws IOException {
        if (image == null) {
            throw new IllegalArgumentException("图片不能为空");
        }
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("最大宽高必须大于 0");
        }
        if (targetSize <= 0) {
            throw new IllegalArgumentException("目标大小必须大于 0");
        }
        if (minQuality <= 0f || minQuality > 1f) {
            throw new IllegalArgumentException("最小质量必须在 0~1 之间");
        }

        BufferedImage scaledImage = image;

        // 缩放图片（等比缩小）
        int width = image.getWidth();
        int height = image.getHeight();

        double scale = Math.min(1.0, Math.min((double) maxWidth / width, (double) maxHeight / height));

        if (scale < 1.0) {
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
            } finally {
                g2d.dispose();
            }
            scaledImage = resized;
        }

        // 质量压缩（JPEG）
        float quality = 1.0f;
        byte[] result = compressJpeg(scaledImage, quality);

        while (result.length > targetSize && quality > minQuality) {
            quality -= 0.05f;
            result = compressJpeg(scaledImage, quality);
        }

        return result;
    }

    // ==================== 2. 添加水印 ====================

    /**
     * 添加文字水印
     *
     * @param source   源图片
     * @param text     水印文字
     * @param fontName 字体名称
     * @param fontSize 字体大小
     * @param alpha    透明度 0~1
     * @param position 水印位置
     * @return 带文字水印的图片
     */
    public static BufferedImage addTextWatermark(BufferedImage source,
                                                 String text,
                                                 String fontName,
                                                 int fontSize,
                                                 float alpha,
                                                 Position position) {
        if (source == null) {
            throw new IllegalArgumentException("源图片不能为空");
        }
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("水印文字不能为空");
        }
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("透明度必须在 0~1");
        }

        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        try {
            g2d.drawImage(source, 0, 0, null);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setFont(new Font(fontName, Font.BOLD, fontSize));
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getHeight();

            int x = 0;
            int y = 0;
            switch (position) {
                case TOP_LEFT:
                    x = 10;
                    y = textHeight;
                    break;
                case TOP_RIGHT:
                    x = source.getWidth() - textWidth - 10;
                    y = textHeight;
                    break;
                case BOTTOM_LEFT:
                    x = 10;
                    y = source.getHeight() - 10;
                    break;
                case BOTTOM_RIGHT:
                    x = source.getWidth() - textWidth - 10;
                    y = source.getHeight() - 10;
                    break;
                case CENTER:
                    x = (source.getWidth() - textWidth) / 2;
                    y = (source.getHeight() + textHeight) / 2 - metrics.getDescent();
                    break;
            }
            g2d.setColor(Color.WHITE);
            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }

        return copy;
    }

    /**
     * 添加图片水印
     *
     * @param source    源图片
     * @param watermark 水印图片
     * @param alpha     透明度 0~1
     * @param position  水印位置
     * @return 带图片水印的图片
     */
    public static BufferedImage addImageWatermark(BufferedImage source,
                                                  BufferedImage watermark,
                                                  float alpha,
                                                  Position position) {
        if (source == null || watermark == null) {
            throw new IllegalArgumentException("源图片和水印图片不能为空");
        }
        if (alpha < 0f || alpha > 1f) {
            throw new IllegalArgumentException("透明度必须在 0~1");
        }

        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = copy.createGraphics();
        try {
            g2d.drawImage(source, 0, 0, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int x = 0;
            int y = 0;
            switch (position) {
                case TOP_LEFT:
                    x = 0;
                    y = 0;
                    break;
                case TOP_RIGHT:
                    x = source.getWidth() - watermark.getWidth();
                    y = 0;
                    break;
                case BOTTOM_LEFT:
                    x = 0;
                    y = source.getHeight() - watermark.getHeight();
                    break;
                case BOTTOM_RIGHT:
                    x = source.getWidth() - watermark.getWidth();
                    y = source.getHeight() - watermark.getHeight();
                    break;
                case CENTER:
                    x = (source.getWidth() - watermark.getWidth()) / 2;
                    y = (source.getHeight() - watermark.getHeight()) / 2;
                    break;
            }

            g2d.drawImage(watermark, x, y, null);
        } finally {
            g2d.dispose();
        }

        return copy;
    }

    // ==================== 3. 获取图片信息 ====================

    /**
     * 获取图片宽度
     *
     * @param image BufferedImage
     * @return 宽度
     */
    public static int getWidth(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片不能为空");
        }
        return image.getWidth();
    }

    /**
     * 获取图片高度
     *
     * @param image BufferedImage
     * @return 高度
     */
    public static int getHeight(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("图片不能为空");
        }
        return image.getHeight();
    }

    /**
     * 获取图片格式
     *
     * @param file 图片文件
     * @return 格式名称，如 "jpg", "png"
     * @throws IOException IO异常
     */
    public static String getFormat(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        try (InputStream is = new FileInputStream(file)) {
            ImageInputStream iis = ImageIO.createImageInputStream(is);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            return readers.next().getFormatName();
        }
    }

    /**
     * 获取图片格式
     *
     * @param path 图片路径
     * @return 格式名称
     * @throws IOException IO异常
     */
    public static String getFormat(String path) throws IOException {
        return getFormat(new File(path));
    }

    /**
     * 获取文件大小（字节）
     *
     * @param file 图片文件
     * @return 文件大小（字节）
     * @throws IOException IO异常
     */
    public static long getFileSize(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        return Files.size(file.toPath());
    }

    /**
     * 获取文件大小（字节）
     *
     * @param path 文件路径
     * @return 文件大小（字节）
     * @throws IOException IO异常
     */
    public static long getFileSize(String path) throws IOException {
        return getFileSize(new File(path));
    }

}
