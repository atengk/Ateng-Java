package io.github.atengk.utils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 图像处理工具类，提供读取、写入、缩放、裁剪、压缩、格式转换、水印、拼接、校验等常用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ImageUtil {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("jpg", "jpeg", "png", "bmp", "gif");
    private static final Set<String> WEB_SAFE_FORMATS = Set.of("jpg", "jpeg", "png", "gif", "bmp");
    private static final DateTimeFormatter FILENAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int DEFAULT_BINARY_THRESHOLD = 128;
    private static final int DEFAULT_HASH_SIZE = 8;
    private static final int DEFAULT_SIMILARITY_SIZE = 64;
    private static final int DEFAULT_BATCH_PREVIEW_SIZE = 200;
    private static final float MIN_QUALITY = 0.1f;
    private static final float MAX_QUALITY = 1.0f;
    private static final int ORIENTATION_NORMAL = 1;
    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_ROTATE_90 = 6;
    private static final int ORIENTATION_ROTATE_270 = 8;

    private ImageUtil() {
        throw new UnsupportedOperationException("ImageUtil 是静态工具类，不允许实例化");
    }

    /**
     * 从文件路径读取图片。
     *
     * @param path 图片路径
     * @return 图片对象
     */
    public static BufferedImage read(Path path) {
        Objects.requireNonNull(path, "图片路径不能为空");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("图片文件不存在或不是普通文件: " + path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return read(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取图片文件失败: " + path, e);
        }
    }

    /**
     * 从输入流读取图片。
     *
     * @param inputStream 图片输入流
     * @return 图片对象
     */
    public static BufferedImage read(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "图片输入流不能为空");
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalArgumentException("输入内容不是可识别的图片");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("读取图片输入流失败", e);
        }
    }

    /**
     * 从字节数组读取图片。
     *
     * @param bytes 图片字节数组
     * @return 图片对象
     */
    public static BufferedImage read(byte[] bytes) {
        requireBytes(bytes, "图片字节数组不能为空");
        return read(new ByteArrayInputStream(bytes));
    }

    /**
     * 安静读取文件图片，读取失败时返回 null。
     *
     * @param path 图片路径
     * @return 图片对象或 null
     */
    public static BufferedImage readQuietly(Path path) {
        try {
            return read(path);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 安静读取流图片，读取失败时返回 null。
     *
     * @param inputStream 图片输入流
     * @return 图片对象或 null
     */
    public static BufferedImage readQuietly(InputStream inputStream) {
        try {
            return read(inputStream);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 安静读取字节图片，读取失败时返回 null。
     *
     * @param bytes 图片字节数组
     * @return 图片对象或 null
     */
    public static BufferedImage readQuietly(byte[] bytes) {
        try {
            return read(bytes);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 将图片写入文件。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @param path   输出路径
     */
    public static void write(BufferedImage image, String format, Path path) {
        Objects.requireNonNull(path, "输出路径不能为空");
        ensureParentDirectory(path);
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            write(image, format, outputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("写入图片文件失败: " + path, e);
        }
    }

    /**
     * 将图片写入输出流。
     *
     * @param image        图片对象
     * @param format       图片格式
     * @param outputStream 输出流
     */
    public static void write(BufferedImage image, String format, OutputStream outputStream) {
        requireImage(image);
        Objects.requireNonNull(outputStream, "图片输出流不能为空");
        String normalizedFormat = normalizeFormat(format);
        BufferedImage outputImage = prepareImageForFormat(image, normalizedFormat, Color.WHITE);
        try {
            boolean written = ImageIO.write(outputImage, normalizedFormat, outputStream);
            if (!written) {
                throw new IllegalArgumentException("当前 JDK 不支持写入图片格式: " + normalizedFormat);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("写入图片输出流失败", e);
        }
    }

    /**
     * 将图片转换为字节数组。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return 图片字节数组
     */
    public static byte[] toBytes(BufferedImage image, String format) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(image, format, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 将图片转换为输入流。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return 图片输入流
     */
    public static InputStream toInputStream(BufferedImage image, String format) {
        return new ByteArrayInputStream(toBytes(image, format));
    }

    /**
     * 获取文件图片格式。
     *
     * @param path 图片路径
     * @return 图片格式
     */
    public static String getFormat(Path path) {
        Objects.requireNonNull(path, "图片路径不能为空");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("图片文件不存在或不是普通文件: " + path);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return getFormat(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("获取图片格式失败: " + path, e);
        }
    }

    /**
     * 获取输入流图片格式。
     *
     * @param inputStream 图片输入流
     * @return 图片格式
     */
    public static String getFormat(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "图片输入流不能为空");
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw new IllegalArgumentException("无法创建图片输入流");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("无法识别图片格式");
            }
            ImageReader reader = readers.next();
            try {
                return normalizeFormat(reader.getFormatName());
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("获取图片格式失败", e);
        }
    }

    /**
     * 获取字节图片格式。
     *
     * @param bytes 图片字节数组
     * @return 图片格式
     */
    public static String getFormat(byte[] bytes) {
        requireBytes(bytes, "图片字节数组不能为空");
        return getFormat(new ByteArrayInputStream(bytes));
    }

    /**
     * 判断文件图片是否为指定格式。
     *
     * @param path   图片路径
     * @param format 图片格式
     * @return 是否为指定格式
     */
    public static boolean isFormat(Path path, String format) {
        return normalizeFormat(getFormat(path)).equals(normalizeFormat(format));
    }

    /**
     * 判断字节图片是否为指定格式。
     *
     * @param bytes  图片字节数组
     * @param format 图片格式
     * @return 是否为指定格式
     */
    public static boolean isFormat(byte[] bytes, String format) {
        return normalizeFormat(getFormat(bytes)).equals(normalizeFormat(format));
    }

    /**
     * 将图片转换为指定格式的字节数组。
     *
     * @param image  图片对象
     * @param format 目标格式
     * @return 图片字节数组
     */
    public static byte[] convert(BufferedImage image, String format) {
        return toBytes(image, format);
    }

    /**
     * 将文件图片转换为指定格式并保存。
     *
     * @param source 源图片路径
     * @param format 目标格式
     * @param target 目标图片路径
     */
    public static void convert(Path source, String format, Path target) {
        write(read(source), format, target);
    }

    /**
     * 将图片转换为 JPG 字节数组。
     *
     * @param image 图片对象
     * @return JPG 字节数组
     */
    public static byte[] toJpg(BufferedImage image) {
        return toBytes(image, "jpg");
    }

    /**
     * 将图片转换为 PNG 字节数组。
     *
     * @param image 图片对象
     * @return PNG 字节数组
     */
    public static byte[] toPng(BufferedImage image) {
        return toBytes(image, "png");
    }

    /**
     * 判断当前 JDK 是否支持指定图片格式读写。
     *
     * @param format 图片格式
     * @return 是否支持
     */
    public static boolean isSupportedFormat(String format) {
        String normalizedFormat;
        try {
            normalizedFormat = normalizeFormat(format);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return ImageIO.getImageReadersByFormatName(normalizedFormat).hasNext()
                || ImageIO.getImageWritersByFormatName(normalizedFormat).hasNext();
    }

    /**
     * 获取图片宽度。
     *
     * @param image 图片对象
     * @return 图片宽度
     */
    public static int getWidth(BufferedImage image) {
        requireImage(image);
        return image.getWidth();
    }

    /**
     * 获取图片高度。
     *
     * @param image 图片对象
     * @return 图片高度
     */
    public static int getHeight(BufferedImage image) {
        requireImage(image);
        return image.getHeight();
    }

    /**
     * 获取图片尺寸。
     *
     * @param image 图片对象
     * @return 图片尺寸
     */
    public static ImageSize getSize(BufferedImage image) {
        requireImage(image);
        return new ImageSize(image.getWidth(), image.getHeight());
    }

    /**
     * 获取文件大小。
     *
     * @param path 文件路径
     * @return 文件字节数
     */
    public static long getFileSize(Path path) {
        Objects.requireNonNull(path, "文件路径不能为空");
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("获取文件大小失败: " + path, e);
        }
    }

    /**
     * 获取图片宽高比。
     *
     * @param image 图片对象
     * @return 宽高比
     */
    public static double getAspectRatio(BufferedImage image) {
        requireImage(image);
        return image.getWidth() * 1.0D / image.getHeight();
    }

    /**
     * 判断图片是否为横图。
     *
     * @param image 图片对象
     * @return 是否为横图
     */
    public static boolean isLandscape(BufferedImage image) {
        requireImage(image);
        return image.getWidth() > image.getHeight();
    }

    /**
     * 判断图片是否为竖图。
     *
     * @param image 图片对象
     * @return 是否为竖图
     */
    public static boolean isPortrait(BufferedImage image) {
        requireImage(image);
        return image.getHeight() > image.getWidth();
    }

    /**
     * 判断图片是否为正方形。
     *
     * @param image 图片对象
     * @return 是否为正方形
     */
    public static boolean isSquare(BufferedImage image) {
        requireImage(image);
        return image.getWidth() == image.getHeight();
    }

    /**
     * 根据图片内容获取 MIME 类型。
     *
     * @param path 图片路径
     * @return MIME 类型
     */
    public static String getMimeType(Path path) {
        return getMimeType(getFormat(path));
    }

    /**
     * 获取文件图片基础信息。
     *
     * @param path 图片路径
     * @return 图片信息
     */
    public static ImageInfo getInfo(Path path) {
        Objects.requireNonNull(path, "图片路径不能为空");
        BufferedImage image = read(path);
        String format = getFormat(path);
        long fileSize = getFileSize(path);
        return new ImageInfo(image.getWidth(), image.getHeight(), format, getMimeType(format), fileSize, getAspectRatio(image));
    }

    /**
     * 按指定宽高缩放图片。
     *
     * @param image  图片对象
     * @param width  目标宽度
     * @param height 目标高度
     * @return 缩放后的图片
     */
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        requireImage(image);
        requirePositive(width, "目标宽度必须大于 0");
        requirePositive(height, "目标高度必须大于 0");
        BufferedImage target = createCompatibleImage(image, width, height);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 按指定宽度等比缩放图片。
     *
     * @param image 图片对象
     * @param width 目标宽度
     * @return 缩放后的图片
     */
    public static BufferedImage resizeByWidth(BufferedImage image, int width) {
        requireImage(image);
        requirePositive(width, "目标宽度必须大于 0");
        int height = Math.max(1, Math.round(image.getHeight() * (width * 1.0f / image.getWidth())));
        return resize(image, width, height);
    }

    /**
     * 按指定高度等比缩放图片。
     *
     * @param image  图片对象
     * @param height 目标高度
     * @return 缩放后的图片
     */
    public static BufferedImage resizeByHeight(BufferedImage image, int height) {
        requireImage(image);
        requirePositive(height, "目标高度必须大于 0");
        int width = Math.max(1, Math.round(image.getWidth() * (height * 1.0f / image.getHeight())));
        return resize(image, width, height);
    }

    /**
     * 按最大边等比缩放图片。
     *
     * @param image   图片对象
     * @param maxSide 最大边长度
     * @return 缩放后的图片
     */
    public static BufferedImage resizeByMaxSide(BufferedImage image, int maxSide) {
        requireImage(image);
        requirePositive(maxSide, "最大边长度必须大于 0");
        int currentMaxSide = Math.max(image.getWidth(), image.getHeight());
        if (currentMaxSide <= maxSide) {
            return copyImage(image);
        }
        return scale(image, maxSide * 1.0d / currentMaxSide);
    }

    /**
     * 按最小边等比缩放图片。
     *
     * @param image   图片对象
     * @param minSide 最小边长度
     * @return 缩放后的图片
     */
    public static BufferedImage resizeByMinSide(BufferedImage image, int minSide) {
        requireImage(image);
        requirePositive(minSide, "最小边长度必须大于 0");
        int currentMinSide = Math.min(image.getWidth(), image.getHeight());
        if (currentMinSide == minSide) {
            return copyImage(image);
        }
        return scale(image, minSide * 1.0d / currentMinSide);
    }

    /**
     * 按比例缩放图片。
     *
     * @param image 图片对象
     * @param scale 缩放比例
     * @return 缩放后的图片
     */
    public static BufferedImage scale(BufferedImage image, double scale) {
        requireImage(image);
        if (scale <= 0 || Double.isNaN(scale) || Double.isInfinite(scale)) {
            throw new IllegalArgumentException("缩放比例必须为大于 0 的有效数字");
        }
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        return resize(image, width, height);
    }

    /**
     * 将图片等比适配到指定范围内，不裁剪。
     *
     * @param image     图片对象
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 适配后的图片
     */
    public static BufferedImage fit(BufferedImage image, int maxWidth, int maxHeight) {
        requireImage(image);
        requirePositive(maxWidth, "最大宽度必须大于 0");
        requirePositive(maxHeight, "最大高度必须大于 0");
        double scale = Math.min(maxWidth * 1.0d / image.getWidth(), maxHeight * 1.0d / image.getHeight());
        if (scale >= 1.0d) {
            return copyImage(image);
        }
        return scale(image, scale);
    }

    /**
     * 将图片等比填满指定尺寸，超出部分居中裁剪。
     *
     * @param image  图片对象
     * @param width  目标宽度
     * @param height 目标高度
     * @return 填满后的图片
     */
    public static BufferedImage fill(BufferedImage image, int width, int height) {
        requireImage(image);
        requirePositive(width, "目标宽度必须大于 0");
        requirePositive(height, "目标高度必须大于 0");
        double scale = Math.max(width * 1.0d / image.getWidth(), height * 1.0d / image.getHeight());
        BufferedImage scaled = scale(image, scale);
        return cropCenter(scaled, width, height);
    }

    /**
     * 判断图片是否需要缩放。
     *
     * @param image     图片对象
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 是否需要缩放
     */
    public static boolean needResize(BufferedImage image, int maxWidth, int maxHeight) {
        requireImage(image);
        requirePositive(maxWidth, "最大宽度必须大于 0");
        requirePositive(maxHeight, "最大高度必须大于 0");
        return image.getWidth() > maxWidth || image.getHeight() > maxHeight;
    }

    /**
     * 按指定坐标和尺寸裁剪图片。
     *
     * @param image  图片对象
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    public static BufferedImage crop(BufferedImage image, int x, int y, int width, int height) {
        requireImage(image);
        if (!isCropAreaValid(image, x, y, width, height)) {
            throw new IllegalArgumentException("裁剪区域超出图片范围或尺寸非法");
        }
        BufferedImage target = createCompatibleImage(image, width, height);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, width, height, x, y, x + width, y + height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 安全裁剪图片，自动修正越界区域。
     *
     * @param image  图片对象
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    public static BufferedImage cropSafe(BufferedImage image, int x, int y, int width, int height) {
        requireImage(image);
        requirePositive(width, "裁剪宽度必须大于 0");
        requirePositive(height, "裁剪高度必须大于 0");
        int safeX = clamp(x, 0, image.getWidth() - 1);
        int safeY = clamp(y, 0, image.getHeight() - 1);
        int safeWidth = Math.min(width, image.getWidth() - safeX);
        int safeHeight = Math.min(height, image.getHeight() - safeY);
        return crop(image, safeX, safeY, safeWidth, safeHeight);
    }

    /**
     * 居中裁剪图片。
     *
     * @param image  图片对象
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的图片
     */
    public static BufferedImage cropCenter(BufferedImage image, int width, int height) {
        requireImage(image);
        requirePositive(width, "裁剪宽度必须大于 0");
        requirePositive(height, "裁剪高度必须大于 0");
        if (width > image.getWidth() || height > image.getHeight()) {
            throw new IllegalArgumentException("居中裁剪尺寸不能超过图片尺寸");
        }
        int x = (image.getWidth() - width) / 2;
        int y = (image.getHeight() - height) / 2;
        return crop(image, x, y, width, height);
    }

    /**
     * 按指定宽高比居中裁剪图片。
     *
     * @param image 图片对象
     * @param ratio 宽高比
     * @return 裁剪后的图片
     */
    public static BufferedImage cropCenterByRatio(BufferedImage image, double ratio) {
        requireImage(image);
        if (ratio <= 0 || Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            throw new IllegalArgumentException("宽高比必须为大于 0 的有效数字");
        }
        double currentRatio = getAspectRatio(image);
        int width = image.getWidth();
        int height = image.getHeight();
        if (currentRatio > ratio) {
            width = Math.max(1, (int) Math.round(height * ratio));
        } else if (currentRatio < ratio) {
            height = Math.max(1, (int) Math.round(width / ratio));
        }
        return cropCenter(image, width, height);
    }

    /**
     * 将图片居中裁剪为正方形。
     *
     * @param image 图片对象
     * @return 正方形图片
     */
    public static BufferedImage cropSquare(BufferedImage image) {
        requireImage(image);
        int size = Math.min(image.getWidth(), image.getHeight());
        return cropCenter(image, size, size);
    }

    /**
     * 将图片裁剪为圆形透明图片。
     *
     * @param image 图片对象
     * @return 圆形图片
     */
    public static BufferedImage cropCircle(BufferedImage image) {
        requireImage(image);
        BufferedImage square = cropSquare(image);
        int size = square.getWidth();
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.setClip(new Ellipse2D.Double(0, 0, size, size));
            graphics.drawImage(square, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 居中裁剪并缩放为指定尺寸。
     *
     * @param image  图片对象
     * @param width  目标宽度
     * @param height 目标高度
     * @return 处理后的图片
     */
    public static BufferedImage cropAndResize(BufferedImage image, int width, int height) {
        return fill(image, width, height);
    }

    /**
     * 判断裁剪区域是否合法。
     *
     * @param image  图片对象
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @return 是否合法
     */
    public static boolean isCropAreaValid(BufferedImage image, int x, int y, int width, int height) {
        requireImage(image);
        return x >= 0 && y >= 0 && width > 0 && height > 0
                && x + width <= image.getWidth()
                && y + height <= image.getHeight();
    }

    /**
     * 按 JPG 质量压缩图片。
     *
     * @param image   图片对象
     * @param quality 压缩质量，范围 0.1 到 1.0
     * @return JPG 字节数组
     */
    public static byte[] compress(BufferedImage image, float quality) {
        requireImage(image);
        requireQuality(quality);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeJpegWithQuality(image, outputStream, quality);
        return outputStream.toByteArray();
    }

    /**
     * 按 JPG 质量压缩文件图片。
     *
     * @param source  源图片路径
     * @param target  目标图片路径
     * @param quality 压缩质量，范围 0.1 到 1.0
     */
    public static void compress(Path source, Path target, float quality) {
        Objects.requireNonNull(source, "源图片路径不能为空");
        Objects.requireNonNull(target, "目标图片路径不能为空");
        ensureParentDirectory(target);
        try {
            Files.write(target, compress(read(source), quality));
        } catch (IOException e) {
            throw new IllegalArgumentException("压缩图片文件失败", e);
        }
    }

    /**
     * 将文件图片压缩到指定大小以内。
     *
     * @param source   源图片路径
     * @param maxBytes 最大字节数
     * @return 压缩后的 JPG 字节数组
     */
    public static byte[] compressToSize(Path source, long maxBytes) {
        Objects.requireNonNull(source, "源图片路径不能为空");
        return compressToSize(FilesToBytes(source), maxBytes);
    }

    /**
     * 将字节图片压缩到指定大小以内。
     *
     * @param bytes    图片字节数组
     * @param maxBytes 最大字节数
     * @return 压缩后的 JPG 字节数组
     */
    public static byte[] compressToSize(byte[] bytes, long maxBytes) {
        requireBytes(bytes, "图片字节数组不能为空");
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("最大字节数必须大于 0");
        }
        if (bytes.length <= maxBytes && isImage(bytes)) {
            return bytes.clone();
        }
        BufferedImage image = read(bytes);
        BufferedImage current = image;
        for (float quality = 0.95f; quality >= MIN_QUALITY; quality -= 0.1f) {
            byte[] result = compress(current, Math.max(MIN_QUALITY, quality));
            if (result.length <= maxBytes) {
                return result;
            }
        }
        double scale = 0.9d;
        current = image;
        while (scale >= 0.1d) {
            current = scale(current, 0.9d);
            byte[] result = compress(current, MIN_QUALITY);
            if (result.length <= maxBytes) {
                return result;
            }
            scale -= 0.1d;
        }
        return compress(current, MIN_QUALITY);
    }

    /**
     * 按最大宽高压缩图片尺寸。
     *
     * @param image     图片对象
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 压缩后的图片
     */
    public static BufferedImage compressBySize(BufferedImage image, int maxWidth, int maxHeight) {
        return fit(image, maxWidth, maxHeight);
    }

    /**
     * 按最大边压缩图片尺寸。
     *
     * @param image   图片对象
     * @param maxSide 最大边长度
     * @return 压缩后的图片
     */
    public static BufferedImage compressByMaxSide(BufferedImage image, int maxSide) {
        return resizeByMaxSide(image, maxSide);
    }

    /**
     * 判断文件图片是否需要压缩。
     *
     * @param path     图片路径
     * @param maxBytes 最大字节数
     * @return 是否需要压缩
     */
    public static boolean needCompress(Path path, long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("最大字节数必须大于 0");
        }
        return getFileSize(path) > maxBytes;
    }

    /**
     * 根据文件大小获取推荐 JPG 压缩质量。
     *
     * @param fileSize 文件字节数
     * @return 推荐质量
     */
    public static float getRecommendedQuality(long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必须大于 0");
        }
        if (fileSize <= 512L * 1024L) {
            return 0.9f;
        }
        if (fileSize <= 2L * 1024L * 1024L) {
            return 0.8f;
        }
        if (fileSize <= 5L * 1024L * 1024L) {
            return 0.65f;
        }
        return 0.5f;
    }

    /**
     * 按指定角度旋转图片。
     *
     * @param image  图片对象
     * @param degree 旋转角度
     * @return 旋转后的图片
     */
    public static BufferedImage rotate(BufferedImage image, double degree) {
        requireImage(image);
        if (Double.isNaN(degree) || Double.isInfinite(degree)) {
            throw new IllegalArgumentException("旋转角度必须为有效数字");
        }
        double radians = Math.toRadians(degree);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = Math.max(1, (int) Math.floor(width * cos + height * sin));
        int newHeight = Math.max(1, (int) Math.floor(height * cos + width * sin));
        BufferedImage target = new BufferedImage(newWidth, newHeight, image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            if (!image.getColorModel().hasAlpha()) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, newWidth, newHeight);
            }
            AffineTransform transform = new AffineTransform();
            transform.translate((newWidth - width) / 2.0d, (newHeight - height) / 2.0d);
            transform.rotate(radians, width / 2.0d, height / 2.0d);
            graphics.drawRenderedImage(image, transform);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 顺时针旋转图片 90 度。
     *
     * @param image 图片对象
     * @return 旋转后的图片
     */
    public static BufferedImage rotateRight(BufferedImage image) {
        return rotate(image, 90);
    }

    /**
     * 逆时针旋转图片 90 度。
     *
     * @param image 图片对象
     * @return 旋转后的图片
     */
    public static BufferedImage rotateLeft(BufferedImage image) {
        return rotate(image, -90);
    }

    /**
     * 旋转图片 180 度。
     *
     * @param image 图片对象
     * @return 旋转后的图片
     */
    public static BufferedImage rotate180(BufferedImage image) {
        return rotate(image, 180);
    }

    /**
     * 水平翻转图片。
     *
     * @param image 图片对象
     * @return 翻转后的图片
     */
    public static BufferedImage flipHorizontal(BufferedImage image) {
        requireImage(image);
        BufferedImage target = createCompatibleImage(image, image.getWidth(), image.getHeight());
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(image, image.getWidth(), 0, -image.getWidth(), image.getHeight(), null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 垂直翻转图片。
     *
     * @param image 图片对象
     * @return 翻转后的图片
     */
    public static BufferedImage flipVertical(BufferedImage image) {
        requireImage(image);
        BufferedImage target = createCompatibleImage(image, image.getWidth(), image.getHeight());
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(image, 0, image.getHeight(), image.getWidth(), -image.getHeight(), null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 获取 EXIF 方向值。JDK 原生 API 不提供完整 EXIF 解析能力，此方法在可读图片上返回正常方向 1。
     *
     * @param path 图片路径
     * @return 方向值
     */
    public static int getOrientation(Path path) {
        checkReadable(path);
        return ORIENTATION_NORMAL;
    }

    /**
     * 获取 EXIF 方向值。JDK 原生 API 不提供完整 EXIF 解析能力，此方法在可读图片上返回正常方向 1。
     *
     * @param bytes 图片字节数组
     * @return 方向值
     */
    public static int getOrientation(byte[] bytes) {
        if (!isImage(bytes)) {
            throw new IllegalArgumentException("输入内容不是可识别的图片");
        }
        return ORIENTATION_NORMAL;
    }

    /**
     * 根据方向值修正图片方向。
     *
     * @param image       图片对象
     * @param orientation 方向值
     * @return 修正后的图片
     */
    public static BufferedImage fixOrientation(BufferedImage image, int orientation) {
        requireImage(image);
        return switch (orientation) {
            case ORIENTATION_ROTATE_180 -> rotate180(image);
            case ORIENTATION_ROTATE_90 -> rotateRight(image);
            case ORIENTATION_ROTATE_270 -> rotateLeft(image);
            default -> copyImage(image);
        };
    }

    /**
     * 添加图片水印。
     *
     * @param image     原图
     * @param watermark 水印图
     * @param x         横坐标
     * @param y         纵坐标
     * @return 添加水印后的图片
     */
    public static BufferedImage addImageWatermark(BufferedImage image, BufferedImage watermark, int x, int y) {
        return addImageWatermark(image, watermark, x, y, 1.0f);
    }

    /**
     * 添加带透明度的图片水印。
     *
     * @param image     原图
     * @param watermark 水印图
     * @param x         横坐标
     * @param y         纵坐标
     * @param alpha     透明度，范围 0 到 1
     * @return 添加水印后的图片
     */
    public static BufferedImage addImageWatermark(BufferedImage image, BufferedImage watermark, int x, int y, float alpha) {
        requireImage(image);
        requireImage(watermark);
        requireAlpha(alpha);
        BufferedImage target = copyImage(image);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            graphics.drawImage(watermark, x, y, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 添加文字水印。
     *
     * @param image 原图
     * @param text  水印文字
     * @param x     横坐标
     * @param y     纵坐标
     * @return 添加水印后的图片
     */
    public static BufferedImage addTextWatermark(BufferedImage image, String text, int x, int y) {
        return drawText(image, text, x, y, new Font(Font.SANS_SERIF, Font.BOLD, 24), Color.WHITE, 0.7f);
    }

    /**
     * 在图片中心添加图片水印。
     *
     * @param image     原图
     * @param watermark 水印图
     * @param alpha     透明度，范围 0 到 1
     * @return 添加水印后的图片
     */
    public static BufferedImage addCenterWatermark(BufferedImage image, BufferedImage watermark, float alpha) {
        requireImage(image);
        requireImage(watermark);
        int x = (image.getWidth() - watermark.getWidth()) / 2;
        int y = (image.getHeight() - watermark.getHeight()) / 2;
        return addImageWatermark(image, watermark, x, y, alpha);
    }

    /**
     * 添加平铺文字水印。
     *
     * @param image   原图
     * @param text    水印文字
     * @param font    字体
     * @param color   颜色
     * @param alpha   透明度，范围 0 到 1
     * @param spacing 平铺间距
     * @return 添加水印后的图片
     */
    public static BufferedImage addTileWatermark(BufferedImage image, String text, Font font, Color color, float alpha, int spacing) {
        requireImage(image);
        requireText(text, "水印文字不能为空");
        Objects.requireNonNull(font, "字体不能为空");
        Objects.requireNonNull(color, "颜色不能为空");
        requireAlpha(alpha);
        requirePositive(spacing, "平铺间距必须大于 0");
        BufferedImage target = copyImage(image);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            graphics.setFont(font);
            graphics.setColor(color);
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = Math.max(1, metrics.stringWidth(text));
            int textHeight = Math.max(1, metrics.getHeight());
            for (int y = textHeight; y < image.getHeight() + textHeight; y += textHeight + spacing) {
                for (int x = 0; x < image.getWidth() + textWidth; x += textWidth + spacing) {
                    graphics.drawString(text, x, y);
                }
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 在右下角添加图片水印。
     *
     * @param image     原图
     * @param watermark 水印图
     * @param margin    边距
     * @param alpha     透明度，范围 0 到 1
     * @return 添加水印后的图片
     */
    public static BufferedImage addBottomRightWatermark(BufferedImage image, BufferedImage watermark, int margin, float alpha) {
        requireImage(image);
        requireImage(watermark);
        requireNonNegative(margin, "边距不能小于 0");
        int x = image.getWidth() - watermark.getWidth() - margin;
        int y = image.getHeight() - watermark.getHeight() - margin;
        return addImageWatermark(image, watermark, x, y, alpha);
    }

    /**
     * 在图片上绘制文字。
     *
     * @param image 原图
     * @param text  文字内容
     * @param x     横坐标
     * @param y     纵坐标
     * @param font  字体
     * @param color 颜色
     * @param alpha 透明度，范围 0 到 1
     * @return 绘制后的图片
     */
    public static BufferedImage drawText(BufferedImage image, String text, int x, int y, Font font, Color color, float alpha) {
        requireImage(image);
        requireText(text, "文字内容不能为空");
        Objects.requireNonNull(font, "字体不能为空");
        Objects.requireNonNull(color, "颜色不能为空");
        requireAlpha(alpha);
        BufferedImage target = copyImage(image);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            graphics.setFont(font);
            graphics.setColor(color);
            graphics.drawString(text, x, y);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 在图片上绘制矩形边框。
     *
     * @param image  原图
     * @param x      横坐标
     * @param y      纵坐标
     * @param width  宽度
     * @param height 高度
     * @param color  颜色
     * @return 绘制后的图片
     */
    public static BufferedImage drawRect(BufferedImage image, int x, int y, int width, int height, Color color) {
        requireImage(image);
        requirePositive(width, "矩形宽度必须大于 0");
        requirePositive(height, "矩形高度必须大于 0");
        Objects.requireNonNull(color, "颜色不能为空");
        BufferedImage target = copyImage(image);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(color);
            graphics.drawRect(x, y, width, height);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 在图片上绘制圆角矩形边框。
     *
     * @param image     原图
     * @param x         横坐标
     * @param y         纵坐标
     * @param width     宽度
     * @param height    高度
     * @param arcWidth  圆角宽度
     * @param arcHeight 圆角高度
     * @param color     颜色
     * @return 绘制后的图片
     */
    public static BufferedImage drawRoundRect(BufferedImage image, int x, int y, int width, int height, int arcWidth, int arcHeight, Color color) {
        requireImage(image);
        requirePositive(width, "矩形宽度必须大于 0");
        requirePositive(height, "矩形高度必须大于 0");
        requireNonNegative(arcWidth, "圆角宽度不能小于 0");
        requireNonNegative(arcHeight, "圆角高度不能小于 0");
        Objects.requireNonNull(color, "颜色不能为空");
        BufferedImage target = copyImage(image);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(color);
            graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 在图片上绘制另一张图片。
     *
     * @param image 原图
     * @param other 待绘制图片
     * @param x     横坐标
     * @param y     纵坐标
     * @return 绘制后的图片
     */
    public static BufferedImage drawImage(BufferedImage image, BufferedImage other, int x, int y) {
        return overlay(image, other, x, y);
    }

    /**
     * 按透明度在图片上绘制另一张图片。
     *
     * @param image 原图
     * @param other 待绘制图片
     * @param x     横坐标
     * @param y     纵坐标
     * @param alpha 透明度，范围 0 到 1
     * @return 绘制后的图片
     */
    public static BufferedImage drawImageWithAlpha(BufferedImage image, BufferedImage other, int x, int y, float alpha) {
        return addImageWatermark(image, other, x, y, alpha);
    }

    /**
     * 将图片转换为灰度图。
     *
     * @param image 图片对象
     * @return 灰度图
     */
    public static BufferedImage toGray(BufferedImage image) {
        requireImage(image);
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 将图片转换为黑白二值图。
     *
     * @param image 图片对象
     * @return 黑白二值图
     */
    public static BufferedImage toBinary(BufferedImage image) {
        requireImage(image);
        BufferedImage gray = toGray(image);
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int value = gray.getRGB(x, y) & 0xFF;
                target.setRGB(x, y, value >= DEFAULT_BINARY_THRESHOLD ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
            }
        }
        return target;
    }

    /**
     * 修改整张图片透明度。
     *
     * @param image 图片对象
     * @param alpha 透明度，范围 0 到 1
     * @return 修改后的图片
     */
    public static BufferedImage changeAlpha(BufferedImage image, float alpha) {
        requireImage(image);
        requireAlpha(alpha);
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 移除透明通道并使用指定背景色填充透明区域。
     *
     * @param image      图片对象
     * @param background 背景色
     * @return 不透明图片
     */
    public static BufferedImage removeAlpha(BufferedImage image, Color background) {
        requireImage(image);
        Objects.requireNonNull(background, "背景色不能为空");
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 为图片填充背景色。
     *
     * @param image 图片对象
     * @param color 背景色
     * @return 处理后的图片
     */
    public static BufferedImage fillBackground(BufferedImage image, Color color) {
        return removeAlpha(image, color);
    }

    /**
     * 替换指定背景色。
     *
     * @param image     图片对象
     * @param source    原颜色
     * @param target    目标颜色
     * @param tolerance 容差，范围 0 到 255
     * @return 处理后的图片
     */
    public static BufferedImage replaceBackground(BufferedImage image, Color source, Color target, int tolerance) {
        requireImage(image);
        Objects.requireNonNull(source, "原颜色不能为空");
        Objects.requireNonNull(target, "目标颜色不能为空");
        if (tolerance < 0 || tolerance > 255) {
            throw new IllegalArgumentException("颜色容差必须在 0 到 255 之间");
        }
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color current = new Color(image.getRGB(x, y), true);
                result.setRGB(x, y, colorDistance(current, source) <= tolerance ? target.getRGB() : current.getRGB());
            }
        }
        return result;
    }

    /**
     * 判断图片是否包含透明通道。
     *
     * @param image 图片对象
     * @return 是否包含透明通道
     */
    public static boolean hasAlpha(BufferedImage image) {
        requireImage(image);
        return image.getColorModel().hasAlpha();
    }

    /**
     * 获取图片主色调。
     *
     * @param image 图片对象
     * @return 主色调
     */
    public static Color getDominantColor(BufferedImage image) {
        requireImage(image);
        return getAverageColor(resizeByMaxSide(image, Math.min(64, Math.max(image.getWidth(), image.getHeight()))));
    }

    /**
     * 对图片进行反色处理。
     *
     * @param image 图片对象
     * @return 反色后的图片
     */
    public static BufferedImage invertColor(BufferedImage image) {
        requireImage(image);
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y), true);
                Color inverted = new Color(255 - c.getRed(), 255 - c.getGreen(), 255 - c.getBlue(), c.getAlpha());
                result.setRGB(x, y, inverted.getRGB());
            }
        }
        return result;
    }

    /**
     * 调整图片亮度。
     *
     * @param image  图片对象
     * @param factor 亮度系数，大于 0
     * @return 处理后的图片
     */
    public static BufferedImage adjustBrightness(BufferedImage image, float factor) {
        requireImage(image);
        if (factor < 0 || Float.isNaN(factor) || Float.isInfinite(factor)) {
            throw new IllegalArgumentException("亮度系数必须为大于等于 0 的有效数字");
        }
        return transformRgb(image, color -> new Color(
                clamp(Math.round(color.getRed() * factor), 0, 255),
                clamp(Math.round(color.getGreen() * factor), 0, 255),
                clamp(Math.round(color.getBlue() * factor), 0, 255),
                color.getAlpha()
        ));
    }

    /**
     * 调整图片对比度。
     *
     * @param image  图片对象
     * @param factor 对比度系数，大于 0
     * @return 处理后的图片
     */
    public static BufferedImage adjustContrast(BufferedImage image, float factor) {
        requireImage(image);
        if (factor < 0 || Float.isNaN(factor) || Float.isInfinite(factor)) {
            throw new IllegalArgumentException("对比度系数必须为大于等于 0 的有效数字");
        }
        return transformRgb(image, color -> new Color(
                clamp(Math.round((color.getRed() - 128) * factor + 128), 0, 255),
                clamp(Math.round((color.getGreen() - 128) * factor + 128), 0, 255),
                clamp(Math.round((color.getBlue() - 128) * factor + 128), 0, 255),
                color.getAlpha()
        ));
    }

    /**
     * 创建指定尺寸缩略图，保持比例并裁剪填满。
     *
     * @param image  图片对象
     * @param width  缩略图宽度
     * @param height 缩略图高度
     * @return 缩略图
     */
    public static BufferedImage thumbnail(BufferedImage image, int width, int height) {
        return thumbnailFill(image, width, height);
    }

    /**
     * 创建适配型缩略图，不裁剪。
     *
     * @param image     图片对象
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @return 缩略图
     */
    public static BufferedImage thumbnailFit(BufferedImage image, int maxWidth, int maxHeight) {
        return fit(image, maxWidth, maxHeight);
    }

    /**
     * 创建填充型缩略图，居中裁剪。
     *
     * @param image  图片对象
     * @param width  目标宽度
     * @param height 目标高度
     * @return 缩略图
     */
    public static BufferedImage thumbnailFill(BufferedImage image, int width, int height) {
        return fill(image, width, height);
    }

    /**
     * 创建正方形头像。
     *
     * @param image 图片对象
     * @param size  头像尺寸
     * @return 头像图片
     */
    public static BufferedImage avatar(BufferedImage image, int size) {
        requirePositive(size, "头像尺寸必须大于 0");
        return fill(image, size, size);
    }

    /**
     * 创建圆形头像。
     *
     * @param image 图片对象
     * @param size  头像尺寸
     * @return 圆形头像图片
     */
    public static BufferedImage circleAvatar(BufferedImage image, int size) {
        return cropCircle(avatar(image, size));
    }

    /**
     * 创建封面图。
     *
     * @param image  图片对象
     * @param width  封面宽度
     * @param height 封面高度
     * @return 封面图
     */
    public static BufferedImage cover(BufferedImage image, int width, int height) {
        return fill(image, width, height);
    }

    /**
     * 创建预览图。
     *
     * @param image   图片对象
     * @param maxSide 最大边长度
     * @return 预览图
     */
    public static BufferedImage preview(BufferedImage image, int maxSide) {
        return resizeByMaxSide(image, maxSide);
    }

    /**
     * 批量创建缩略图。
     *
     * @param images 图片集合
     * @param width  缩略图宽度
     * @param height 缩略图高度
     * @return 缩略图集合
     */
    public static List<BufferedImage> thumbnailBatch(Collection<BufferedImage> images, int width, int height) {
        requireImageCollection(images);
        return images.stream().map(image -> thumbnail(image, width, height)).toList();
    }

    /**
     * 创建指定背景色的画布。
     *
     * @param width      画布宽度
     * @param height     画布高度
     * @param background 背景色
     * @return 画布图片
     */
    public static BufferedImage createCanvas(int width, int height, Color background) {
        requirePositive(width, "画布宽度必须大于 0");
        requirePositive(height, "画布高度必须大于 0");
        Objects.requireNonNull(background, "背景色不能为空");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(background);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /**
     * 创建透明画布。
     *
     * @param width  画布宽度
     * @param height 画布高度
     * @return 透明画布
     */
    public static BufferedImage createTransparentCanvas(int width, int height) {
        requirePositive(width, "画布宽度必须大于 0");
        requirePositive(height, "画布高度必须大于 0");
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * 横向拼接多张图片。
     *
     * @param images 图片集合
     * @return 拼接后的图片
     */
    public static BufferedImage concatHorizontal(List<BufferedImage> images) {
        requireImageList(images);
        int width = images.stream().mapToInt(BufferedImage::getWidth).sum();
        int height = images.stream().mapToInt(BufferedImage::getHeight).max().orElseThrow();
        BufferedImage target = createTransparentCanvas(width, height);
        Graphics2D graphics = target.createGraphics();
        try {
            int x = 0;
            for (BufferedImage image : images) {
                graphics.drawImage(image, x, 0, null);
                x += image.getWidth();
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 纵向拼接多张图片。
     *
     * @param images 图片集合
     * @return 拼接后的图片
     */
    public static BufferedImage concatVertical(List<BufferedImage> images) {
        requireImageList(images);
        int width = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int height = images.stream().mapToInt(BufferedImage::getHeight).sum();
        BufferedImage target = createTransparentCanvas(width, height);
        Graphics2D graphics = target.createGraphics();
        try {
            int y = 0;
            for (BufferedImage image : images) {
                graphics.drawImage(image, 0, y, null);
                y += image.getHeight();
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 按网格拼接多张图片。
     *
     * @param images  图片集合
     * @param columns 列数
     * @return 拼接后的图片
     */
    public static BufferedImage concatGrid(List<BufferedImage> images, int columns) {
        requireImageList(images);
        requirePositive(columns, "列数必须大于 0");
        int cellWidth = images.stream().mapToInt(BufferedImage::getWidth).max().orElseThrow();
        int cellHeight = images.stream().mapToInt(BufferedImage::getHeight).max().orElseThrow();
        int rows = (int) Math.ceil(images.size() * 1.0d / columns);
        BufferedImage target = createTransparentCanvas(cellWidth * columns, cellHeight * rows);
        Graphics2D graphics = target.createGraphics();
        try {
            for (int i = 0; i < images.size(); i++) {
                int row = i / columns;
                int column = i % columns;
                graphics.drawImage(images.get(i), column * cellWidth, row * cellHeight, null);
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 将图片叠加到另一张图片上。
     *
     * @param base    底图
     * @param overlay 叠加图
     * @param x       横坐标
     * @param y       纵坐标
     * @return 合成后的图片
     */
    public static BufferedImage overlay(BufferedImage base, BufferedImage overlay, int x, int y) {
        requireImage(base);
        requireImage(overlay);
        BufferedImage target = copyImage(base);
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.drawImage(overlay, x, y, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 将图片居中叠加到另一张图片上。
     *
     * @param base    底图
     * @param overlay 叠加图
     * @return 合成后的图片
     */
    public static BufferedImage overlayCenter(BufferedImage base, BufferedImage overlay) {
        requireImage(base);
        requireImage(overlay);
        return overlay(base, overlay, (base.getWidth() - overlay.getWidth()) / 2, (base.getHeight() - overlay.getHeight()) / 2);
    }

    /**
     * 为图片添加边距。
     *
     * @param image   图片对象
     * @param padding 边距
     * @param color   背景色
     * @return 添加边距后的图片
     */
    public static BufferedImage padding(BufferedImage image, int padding, Color color) {
        requireImage(image);
        requireNonNegative(padding, "边距不能小于 0");
        Objects.requireNonNull(color, "背景色不能为空");
        BufferedImage target = createCanvas(image.getWidth() + padding * 2, image.getHeight() + padding * 2, color);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.drawImage(image, padding, padding, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 为图片添加边框。
     *
     * @param image 图片对象
     * @param width 边框宽度
     * @param color 边框颜色
     * @return 添加边框后的图片
     */
    public static BufferedImage border(BufferedImage image, int width, Color color) {
        requireImage(image);
        requireNonNegative(width, "边框宽度不能小于 0");
        Objects.requireNonNull(color, "边框颜色不能为空");
        BufferedImage target = padding(image, width, color);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(color);
            for (int i = 0; i < width; i++) {
                graphics.drawRect(i, i, target.getWidth() - 1 - i * 2, target.getHeight() - 1 - i * 2);
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    /**
     * 将图片转换为 Base64 字符串。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return Base64 字符串
     */
    public static String toBase64(BufferedImage image, String format) {
        return toBase64(toBytes(image, format));
    }

    /**
     * 将字节数组转换为 Base64 字符串。
     *
     * @param bytes 字节数组
     * @return Base64 字符串
     */
    public static String toBase64(byte[] bytes) {
        requireBytes(bytes, "字节数组不能为空");
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 将 Base64 字符串转换为图片。
     *
     * @param base64 Base64 字符串
     * @return 图片对象
     */
    public static BufferedImage fromBase64(String base64) {
        requireText(base64, "Base64 字符串不能为空");
        return read(Base64.getDecoder().decode(removeDataUrlPrefix(base64)));
    }

    /**
     * 将图片转换为 Data URL。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return Data URL 字符串
     */
    public static String toDataUrl(BufferedImage image, String format) {
        String normalizedFormat = normalizeFormat(format);
        return "data:" + getMimeType(normalizedFormat) + ";base64," + toBase64(image, normalizedFormat);
    }

    /**
     * 将 Data URL 转换为图片。
     *
     * @param dataUrl Data URL 字符串
     * @return 图片对象
     */
    public static BufferedImage fromDataUrl(String dataUrl) {
        requireText(dataUrl, "Data URL 不能为空");
        if (!dataUrl.startsWith("data:image/") || !dataUrl.contains(",")) {
            throw new IllegalArgumentException("Data URL 格式错误");
        }
        return fromBase64(removeDataUrlPrefix(dataUrl));
    }

    /**
     * 判断字符串是否为 Base64 图片。
     *
     * @param value 字符串
     * @return 是否为 Base64 图片
     */
    public static boolean isBase64Image(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return isImage(Base64.getDecoder().decode(removeDataUrlPrefix(value)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取 Data URL 中的 MIME 类型。
     *
     * @param dataUrl Data URL 字符串
     * @return MIME 类型
     */
    public static String getDataUrlMimeType(String dataUrl) {
        requireText(dataUrl, "Data URL 不能为空");
        int semicolonIndex = dataUrl.indexOf(';');
        if (!dataUrl.startsWith("data:") || semicolonIndex <= "data:".length()) {
            throw new IllegalArgumentException("Data URL 格式错误");
        }
        return dataUrl.substring("data:".length(), semicolonIndex);
    }

    /**
     * 移除 Data URL 前缀。
     *
     * @param dataUrl Data URL 或 Base64 字符串
     * @return Base64 字符串
     */
    public static String removeDataUrlPrefix(String dataUrl) {
        requireText(dataUrl, "Data URL 不能为空");
        int commaIndex = dataUrl.indexOf(',');
        if (dataUrl.startsWith("data:") && commaIndex >= 0) {
            return dataUrl.substring(commaIndex + 1);
        }
        return dataUrl;
    }

    /**
     * 判断文件是否为图片。
     *
     * @param path 文件路径
     * @return 是否为图片
     */
    public static boolean isImage(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        try {
            getFormat(path);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断字节数组是否为图片。
     *
     * @param bytes 字节数组
     * @return 是否为图片
     */
    public static boolean isImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        try {
            getFormat(bytes);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 校验文件图片格式。
     *
     * @param path           图片路径
     * @param allowFormats   允许的格式
     */
    public static void checkFormat(Path path, String... allowFormats) {
        requireFormats(allowFormats);
        String format = getFormat(path);
        if (!containsFormat(format, allowFormats)) {
            throw new IllegalArgumentException("图片格式不允许: " + format);
        }
    }

    /**
     * 校验字节图片格式。
     *
     * @param bytes        图片字节数组
     * @param allowFormats 允许的格式
     */
    public static void checkFormat(byte[] bytes, String... allowFormats) {
        requireFormats(allowFormats);
        String format = getFormat(bytes);
        if (!containsFormat(format, allowFormats)) {
            throw new IllegalArgumentException("图片格式不允许: " + format);
        }
    }

    /**
     * 校验文件大小。
     *
     * @param path     文件路径
     * @param maxBytes 最大字节数
     */
    public static void checkFileSize(Path path, long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("最大字节数必须大于 0");
        }
        long size = getFileSize(path);
        if (size > maxBytes) {
            throw new IllegalArgumentException("文件大小超过限制: " + size + " > " + maxBytes);
        }
    }

    /**
     * 校验图片最大宽高。
     *
     * @param image     图片对象
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     */
    public static void checkDimension(BufferedImage image, int maxWidth, int maxHeight) {
        requireImage(image);
        requirePositive(maxWidth, "最大宽度必须大于 0");
        requirePositive(maxHeight, "最大高度必须大于 0");
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            throw new IllegalArgumentException("图片尺寸超过限制");
        }
    }

    /**
     * 校验图片最大像素数。
     *
     * @param image     图片对象
     * @param maxPixels 最大像素数
     */
    public static void checkMaxPixels(BufferedImage image, long maxPixels) {
        requireImage(image);
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("最大像素数必须大于 0");
        }
        long pixels = (long) image.getWidth() * image.getHeight();
        if (pixels > maxPixels) {
            throw new IllegalArgumentException("图片像素数超过限制: " + pixels + " > " + maxPixels);
        }
    }

    /**
     * 判断文件是否超过指定大小。
     *
     * @param path     文件路径
     * @param maxBytes 最大字节数
     * @return 是否超过
     */
    public static boolean isOverSize(Path path, long maxBytes) {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("最大字节数必须大于 0");
        }
        return getFileSize(path) > maxBytes;
    }

    /**
     * 判断图片是否超过指定像素数。
     *
     * @param image     图片对象
     * @param maxPixels 最大像素数
     * @return 是否超过
     */
    public static boolean isOverPixels(BufferedImage image, long maxPixels) {
        requireImage(image);
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("最大像素数必须大于 0");
        }
        return (long) image.getWidth() * image.getHeight() > maxPixels;
    }

    /**
     * 校验图片文件是否可读。
     *
     * @param path 图片路径
     */
    public static void checkReadable(Path path) {
        if (!isImage(path)) {
            throw new IllegalArgumentException("文件不是可识别的图片: " + path);
        }
    }

    /**
     * 重新编码图片以清理异常元数据。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return 重新编码后的字节数组
     */
    public static byte[] sanitize(BufferedImage image, String format) {
        return toBytes(image, format);
    }

    /**
     * 获取文件扩展名。
     *
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getExtension(String filename) {
        requireText(filename, "文件名不能为空");
        int slashIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 根据图片格式获取推荐扩展名。
     *
     * @param format 图片格式
     * @return 扩展名
     */
    public static String getExtensionByFormat(String format) {
        return switch (normalizeFormat(format)) {
            case "jpeg" -> "jpg";
            default -> normalizeFormat(format);
        };
    }

    /**
     * 根据图片格式获取 MIME 类型。
     *
     * @param format 图片格式
     * @return MIME 类型
     */
    public static String getMimeType(String format) {
        return switch (normalizeFormat(format)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 根据 MIME 类型获取图片格式。
     *
     * @param mimeType MIME 类型
     * @return 图片格式
     */
    public static String getFormatByMimeType(String mimeType) {
        requireText(mimeType, "MIME 类型不能为空");
        String normalized = mimeType.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/bmp", "image/x-ms-bmp" -> "bmp";
            default -> throw new IllegalArgumentException("不支持的图片 MIME 类型: " + mimeType);
        };
    }

    /**
     * 规范化图片格式名。
     *
     * @param format 图片格式
     * @return 规范化后的格式
     */
    public static String normalizeFormat(String format) {
        requireText(format, "图片格式不能为空");
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if ("image/jpeg".equals(normalized) || "jpeg".equals(normalized)) {
            return "jpg";
        }
        if ("image/png".equals(normalized)) {
            return "png";
        }
        if ("image/gif".equals(normalized)) {
            return "gif";
        }
        if ("image/bmp".equals(normalized) || "image/x-ms-bmp".equals(normalized)) {
            return "bmp";
        }
        if (!normalized.matches("[a-z0-9]+")) {
            throw new IllegalArgumentException("图片格式不合法: " + format);
        }
        return normalized;
    }

    /**
     * 生成图片文件名。
     *
     * @param format 图片格式
     * @return 图片文件名
     */
    public static String generateFilename(String format) {
        return LocalDateTime.now().format(FILENAME_TIME_FORMATTER) + "-" + UUID.randomUUID() + "." + getExtensionByFormat(format);
    }

    /**
     * 判断扩展名是否为支持的图片扩展名。
     *
     * @param extension 扩展名
     * @return 是否支持
     */
    public static boolean isSupportedExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return SUPPORTED_FORMATS.contains(normalizeFormat(extension));
    }

    /**
     * 判断 MIME 类型是否为支持的图片 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return 是否支持
     */
    public static boolean isSupportedMimeType(String mimeType) {
        try {
            return SUPPORTED_FORMATS.contains(getFormatByMimeType(mimeType));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断图片是否为空白图片。
     *
     * @param image 图片对象
     * @return 是否为空白图片
     */
    public static boolean isBlank(BufferedImage image) {
        requireImage(image);
        int firstRgb = image.getRGB(0, 0);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != firstRgb) {
                    return false;
                }
            }
        }
        Color color = new Color(firstRgb, true);
        return color.getAlpha() == 0 || color.equals(Color.WHITE) || color.equals(new Color(255, 255, 255, 0));
    }

    /**
     * 判断图片是否为全透明图片。
     *
     * @param image 图片对象
     * @return 是否为全透明图片
     */
    public static boolean isTransparent(BufferedImage image) {
        requireImage(image);
        if (!hasAlpha(image)) {
            return false;
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >> 24) & 0xff) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 判断图片是否为纯色图片。
     *
     * @param image 图片对象
     * @return 是否为纯色图片
     */
    public static boolean isSolidColor(BufferedImage image) {
        requireImage(image);
        int firstRgb = image.getRGB(0, 0);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != firstRgb) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取图片平均颜色。
     *
     * @param image 图片对象
     * @return 平均颜色
     */
    public static Color getAverageColor(BufferedImage image) {
        requireImage(image);
        long red = 0;
        long green = 0;
        long blue = 0;
        long alpha = 0;
        long count = (long) image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);
                red += color.getRed();
                green += color.getGreen();
                blue += color.getBlue();
                alpha += color.getAlpha();
            }
        }
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count), (int) (alpha / count));
    }

    /**
     * 获取图片平均亮度。
     *
     * @param image 图片对象
     * @return 平均亮度，范围 0 到 255
     */
    public static double getBrightness(BufferedImage image) {
        Color color = getAverageColor(image);
        return 0.299d * color.getRed() + 0.587d * color.getGreen() + 0.114d * color.getBlue();
    }

    /**
     * 判断图片是否偏暗。
     *
     * @param image 图片对象
     * @return 是否偏暗
     */
    public static boolean isDark(BufferedImage image) {
        return getBrightness(image) < 85;
    }

    /**
     * 判断图片是否偏亮。
     *
     * @param image 图片对象
     * @return 是否偏亮
     */
    public static boolean isBright(BufferedImage image) {
        return getBrightness(image) > 170;
    }

    /**
     * 计算两张图片的简单相似度。
     *
     * @param image1 图片一
     * @param image2 图片二
     * @return 相似度，范围 0 到 1
     */
    public static double similarity(BufferedImage image1, BufferedImage image2) {
        requireImage(image1);
        requireImage(image2);
        BufferedImage a = resize(image1, DEFAULT_SIMILARITY_SIZE, DEFAULT_SIMILARITY_SIZE);
        BufferedImage b = resize(image2, DEFAULT_SIMILARITY_SIZE, DEFAULT_SIMILARITY_SIZE);
        long diff = 0;
        long maxDiff = 255L * 3L * DEFAULT_SIMILARITY_SIZE * DEFAULT_SIMILARITY_SIZE;
        for (int y = 0; y < DEFAULT_SIMILARITY_SIZE; y++) {
            for (int x = 0; x < DEFAULT_SIMILARITY_SIZE; x++) {
                Color ca = new Color(a.getRGB(x, y));
                Color cb = new Color(b.getRGB(x, y));
                diff += Math.abs(ca.getRed() - cb.getRed());
                diff += Math.abs(ca.getGreen() - cb.getGreen());
                diff += Math.abs(ca.getBlue() - cb.getBlue());
            }
        }
        return Math.max(0d, 1d - diff * 1.0d / maxDiff);
    }

    /**
     * 计算图片平均哈希值。
     *
     * @param image 图片对象
     * @return 64 位二进制哈希字符串
     */
    public static String perceptualHash(BufferedImage image) {
        requireImage(image);
        BufferedImage gray = toGray(resize(image, DEFAULT_HASH_SIZE, DEFAULT_HASH_SIZE));
        int[] values = new int[DEFAULT_HASH_SIZE * DEFAULT_HASH_SIZE];
        int sum = 0;
        int index = 0;
        for (int y = 0; y < DEFAULT_HASH_SIZE; y++) {
            for (int x = 0; x < DEFAULT_HASH_SIZE; x++) {
                int value = gray.getRGB(x, y) & 0xff;
                values[index++] = value;
                sum += value;
            }
        }
        int average = sum / values.length;
        StringBuilder builder = new StringBuilder(values.length);
        for (int value : values) {
            builder.append(value >= average ? '1' : '0');
        }
        return builder.toString();
    }

    /**
     * 批量缩放图片。
     *
     * @param images 图片集合
     * @param width  目标宽度
     * @param height 目标高度
     * @return 缩放后的图片集合
     */
    public static List<BufferedImage> resizeBatch(Collection<BufferedImage> images, int width, int height) {
        requireImageCollection(images);
        return images.stream().map(image -> resize(image, width, height)).toList();
    }

    /**
     * 批量压缩图片字节数组。
     *
     * @param images   图片字节数组集合
     * @param maxBytes 最大字节数
     * @return 压缩后的字节数组集合
     */
    public static List<byte[]> compressBatch(Collection<byte[]> images, long maxBytes) {
        Objects.requireNonNull(images, "图片集合不能为空");
        if (images.stream().anyMatch(bytes -> bytes == null || bytes.length == 0)) {
            throw new IllegalArgumentException("图片集合不能包含空字节数组");
        }
        return images.stream().map(bytes -> compressToSize(bytes, maxBytes)).toList();
    }

    /**
     * 批量转换图片格式。
     *
     * @param images 图片集合
     * @param format 目标格式
     * @return 转换后的字节数组集合
     */
    public static List<byte[]> convertBatch(Collection<BufferedImage> images, String format) {
        requireImageCollection(images);
        return images.stream().map(image -> convert(image, format)).toList();
    }

    /**
     * 遍历目录中的图片文件。
     *
     * @param directory 目录路径
     * @return 图片文件路径集合
     */
    public static List<Path> walkImages(Path directory) {
        Objects.requireNonNull(directory, "目录路径不能为空");
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("路径不是目录: " + directory);
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).filter(ImageUtil::isImage).toList();
        } catch (IOException e) {
            throw new IllegalArgumentException("遍历图片目录失败: " + directory, e);
        }
    }

    /**
     * 批量校验图片文件是否可读。
     *
     * @param paths 图片路径集合
     * @return 处理结果集合
     */
    public static List<ImageProcessResult> checkBatch(Collection<Path> paths) {
        requirePathCollection(paths);
        List<ImageProcessResult> results = new ArrayList<>();
        for (Path path : paths) {
            long start = System.currentTimeMillis();
            try {
                checkReadable(path);
                results.add(ImageProcessResult.success(path, path, System.currentTimeMillis() - start));
            } catch (RuntimeException e) {
                results.add(ImageProcessResult.failure(path, path, e.getMessage(), System.currentTimeMillis() - start));
            }
        }
        return results;
    }

    /**
     * 批量处理图片文件并输出到指定目录。
     *
     * @param sources         源图片路径集合
     * @param targetDirectory 目标目录
     * @param format          目标格式
     * @param maxWidth        最大宽度
     * @param maxHeight       最大高度
     * @return 处理结果集合
     */
    public static List<ImageProcessResult> processBatch(Collection<Path> sources, Path targetDirectory, String format, int maxWidth, int maxHeight) {
        requirePathCollection(sources);
        Objects.requireNonNull(targetDirectory, "目标目录不能为空");
        ensureDirectory(targetDirectory);
        String normalizedFormat = normalizeFormat(format);
        List<ImageProcessResult> results = new ArrayList<>();
        for (Path source : sources) {
            long start = System.currentTimeMillis();
            Path target = targetDirectory.resolve(stripExtension(source.getFileName().toString()) + "." + getExtensionByFormat(normalizedFormat));
            try {
                BufferedImage image = fit(read(source), maxWidth, maxHeight);
                write(image, normalizedFormat, target);
                results.add(ImageProcessResult.success(source, target, System.currentTimeMillis() - start));
            } catch (RuntimeException e) {
                results.add(ImageProcessResult.failure(source, target, e.getMessage(), System.currentTimeMillis() - start));
            }
        }
        return results;
    }

    /**
     * 获取图片响应 Content-Type。
     *
     * @param format 图片格式
     * @return Content-Type
     */
    public static String getContentType(String format) {
        return getMimeType(format);
    }

    /**
     * 获取下载文件名。
     *
     * @param filename 原文件名
     * @param format   图片格式
     * @return 下载文件名
     */
    public static String getDownloadFilename(String filename, String format) {
        requireText(filename, "文件名不能为空");
        return stripExtension(filename) + "." + getExtensionByFormat(format);
    }

    /**
     * 将图片转换为接口响应字节数组。
     *
     * @param image  图片对象
     * @param format 图片格式
     * @return 响应字节数组
     */
    public static byte[] toResponseBytes(BufferedImage image, String format) {
        return toBytes(image, format);
    }

    /**
     * 判断图片格式是否适合 Web 直接展示。
     *
     * @param format 图片格式
     * @return 是否适合 Web 展示
     */
    public static boolean isWebSafeFormat(String format) {
        try {
            return WEB_SAFE_FORMATS.contains(normalizeFormat(format));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 生成图片处理缓存键。
     *
     * @param sourceKey 源图片标识
     * @param operation 操作名
     * @param args      操作参数
     * @return 缓存键
     */
    public static String getCacheKey(String sourceKey, String operation, Object... args) {
        requireText(sourceKey, "源图片标识不能为空");
        requireText(operation, "操作名不能为空");
        StringBuilder builder = new StringBuilder(sourceKey).append(':').append(operation);
        if (args != null) {
            for (Object arg : args) {
                builder.append(':').append(String.valueOf(arg));
            }
        }
        return builder.toString();
    }

    private static byte[] FilesToBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件字节失败: " + path, e);
        }
    }

    private static BufferedImage prepareImageForFormat(BufferedImage image, String format, Color background) {
        if (("jpg".equals(format) || "jpeg".equals(format) || "bmp".equals(format)) && image.getColorModel().hasAlpha()) {
            return removeAlpha(image, background);
        }
        return image;
    }

    private static BufferedImage createCompatibleImage(BufferedImage image, int width, int height) {
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        return new BufferedImage(width, height, type);
    }

    private static BufferedImage copyImage(BufferedImage image) {
        requireImage(image);
        BufferedImage target = createCompatibleImage(image, image.getWidth(), image.getHeight());
        Graphics2D graphics = target.createGraphics();
        try {
            applyQualityHints(graphics);
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static void writeJpegWithQuality(BufferedImage image, OutputStream outputStream, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("当前 JDK 不支持 JPG 写入");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(prepareImageForFormat(image, "jpg", Color.WHITE), null, null), param);
        } catch (IOException e) {
            throw new IllegalArgumentException("按质量写入 JPG 图片失败", e);
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage transformRgb(BufferedImage image, Function<Color, Color> transformer) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                result.setRGB(x, y, transformer.apply(new Color(image.getRGB(x, y), true)).getRGB());
            }
        }
        return result;
    }

    private static int colorDistance(Color a, Color b) {
        return Math.max(Math.max(Math.abs(a.getRed() - b.getRed()), Math.abs(a.getGreen() - b.getGreen())), Math.abs(a.getBlue() - b.getBlue()));
    }

    private static void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static void ensureParentDirectory(Path path) {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            ensureDirectory(parent);
        }
    }

    private static void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalArgumentException("创建目录失败: " + directory, e);
        }
    }

    private static String stripExtension(String filename) {
        requireText(filename, "文件名不能为空");
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private static boolean containsFormat(String format, String... allowFormats) {
        String normalized = normalizeFormat(format);
        for (String allowFormat : allowFormats) {
            if (normalized.equals(normalizeFormat(allowFormat))) {
                return true;
            }
        }
        return false;
    }

    private static void requireFormats(String... allowFormats) {
        if (allowFormats == null || allowFormats.length == 0) {
            throw new IllegalArgumentException("允许的图片格式不能为空");
        }
        for (String format : allowFormats) {
            normalizeFormat(format);
        }
    }

    private static void requireImage(BufferedImage image) {
        Objects.requireNonNull(image, "图片对象不能为空");
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("图片宽高必须大于 0");
        }
    }

    private static void requireBytes(byte[] bytes, String message) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireText(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireQuality(float quality) {
        if (quality < MIN_QUALITY || quality > MAX_QUALITY || Float.isNaN(quality)) {
            throw new IllegalArgumentException("压缩质量必须在 0.1 到 1.0 之间");
        }
    }

    private static void requireAlpha(float alpha) {
        if (alpha < 0 || alpha > 1 || Float.isNaN(alpha)) {
            throw new IllegalArgumentException("透明度必须在 0 到 1 之间");
        }
    }

    private static void requireImageCollection(Collection<BufferedImage> images) {
        Objects.requireNonNull(images, "图片集合不能为空");
        if (images.isEmpty() || images.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("图片集合不能为空且不能包含空图片");
        }
    }

    private static void requireImageList(List<BufferedImage> images) {
        requireImageCollection(images);
    }

    private static void requirePathCollection(Collection<Path> paths) {
        Objects.requireNonNull(paths, "路径集合不能为空");
        if (paths.isEmpty() || paths.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("路径集合不能为空且不能包含空路径");
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 缩放模式。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public enum ResizeMode {
        /** 强制缩放。 */
        RESIZE,
        /** 等比适配。 */
        FIT,
        /** 等比填充。 */
        FILL
    }

    /**
     * 图片尺寸。
     *
     * @param width  宽度
     * @param height 高度
     * @author Ateng
     * @since 2026-04-30
     */
    public record ImageSize(int width, int height) {
        /**
         * 创建图片尺寸对象。
         */
        public ImageSize {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("图片尺寸必须大于 0");
            }
        }

        /**
         * 转换为 AWT 尺寸对象。
         *
         * @return AWT 尺寸对象
         */
        public Dimension toDimension() {
            return new Dimension(width, height);
        }
    }

    /**
     * 图片基础信息。
     *
     * @param width       宽度
     * @param height      高度
     * @param format      格式
     * @param mimeType    MIME 类型
     * @param fileSize    文件大小
     * @param aspectRatio 宽高比
     * @author Ateng
     * @since 2026-04-30
     */
    public record ImageInfo(int width, int height, String format, String mimeType, long fileSize, double aspectRatio) {
        /**
         * 创建图片基础信息对象。
         */
        public ImageInfo {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("图片宽高必须大于 0");
            }
            requireText(format, "图片格式不能为空");
            requireText(mimeType, "MIME 类型不能为空");
            if (fileSize < 0) {
                throw new IllegalArgumentException("文件大小不能小于 0");
            }
        }
    }

    /**
     * 图片处理结果。
     *
     * @param source     源路径
     * @param target     目标路径
     * @param success    是否成功
     * @param message    处理消息
     * @param costMillis 耗时毫秒数
     * @author Ateng
     * @since 2026-04-30
     */
    public record ImageProcessResult(Path source, Path target, boolean success, String message, long costMillis) {
        /**
         * 创建图片处理结果对象。
         */
        public ImageProcessResult {
            Objects.requireNonNull(source, "源路径不能为空");
            if (costMillis < 0) {
                throw new IllegalArgumentException("耗时不能小于 0");
            }
        }

        /**
         * 创建成功结果。
         *
         * @param source     源路径
         * @param target     目标路径
         * @param costMillis 耗时毫秒数
         * @return 处理结果
         */
        public static ImageProcessResult success(Path source, Path target, long costMillis) {
            return new ImageProcessResult(source, target, true, "处理成功", costMillis);
        }

        /**
         * 创建失败结果。
         *
         * @param source     源路径
         * @param target     目标路径
         * @param message    失败消息
         * @param costMillis 耗时毫秒数
         * @return 处理结果
         */
        public static ImageProcessResult failure(Path source, Path target, String message, long costMillis) {
            return new ImageProcessResult(source, target, false, message, costMillis);
        }
    }

    /**
     * 水印配置。
     *
     * @param alpha  透明度
     * @param margin 边距
     * @param font   字体
     * @param color  颜色
     * @author Ateng
     * @since 2026-04-30
     */
    public record WatermarkOptions(float alpha, int margin, Font font, Color color) {
        /**
         * 创建水印配置对象。
         */
        public WatermarkOptions {
            requireAlpha(alpha);
            requireNonNegative(margin, "边距不能小于 0");
            Objects.requireNonNull(font, "字体不能为空");
            Objects.requireNonNull(color, "颜色不能为空");
        }
    }

    /**
     * 压缩配置。
     *
     * @param quality   JPG 质量
     * @param maxWidth  最大宽度
     * @param maxHeight 最大高度
     * @param maxBytes  最大字节数
     * @author Ateng
     * @since 2026-04-30
     */
    public record CompressOptions(float quality, int maxWidth, int maxHeight, long maxBytes) {
        /**
         * 创建压缩配置对象。
         */
        public CompressOptions {
            requireQuality(quality);
            requirePositive(maxWidth, "最大宽度必须大于 0");
            requirePositive(maxHeight, "最大高度必须大于 0");
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("最大字节数必须大于 0");
            }
        }
    }

    /**
     * 裁剪配置。
     *
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  裁剪宽度
     * @param height 裁剪高度
     * @param safe   是否安全裁剪
     * @author Ateng
     * @since 2026-04-30
     */
    public record CropOptions(int x, int y, int width, int height, boolean safe) {
        /**
         * 创建裁剪配置对象。
         */
        public CropOptions {
            requirePositive(width, "裁剪宽度必须大于 0");
            requirePositive(height, "裁剪高度必须大于 0");
        }
    }
}
