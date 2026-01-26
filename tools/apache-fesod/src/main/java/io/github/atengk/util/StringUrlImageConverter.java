package io.github.atengk.util;

import org.apache.fesod.common.util.IoUtils;
import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 自定义 Excel 图片转换器，用于将 URL 字符串转换为 Excel 图片。
 * 该转换器支持：
 * 1. 通过 URL 下载图片并插入到 Excel 单元格中。
 * 2. 如果图片 URL 为空或无效，则使用默认图片 URL 进行替代。
 * 3. 保障 Excel 生成时不会因图片下载失败而导致异常。
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class StringUrlImageConverter implements Converter<String> {

    /**
     * 默认图片 URL（当原始 URL 无效或下载失败时使用）
     */
    private static final String DEFAULT_IMAGE_URL = "https://placehold.co/100x100/png?text=Default";
    private static final Logger log = LoggerFactory.getLogger(StringUrlImageConverter.class);

    /**
     * 将 URL 字符串转换为 Excel 可用的图片数据。
     *
     * @param url                 图片的 URL 字符串
     * @param contentProperty     Excel 内容属性（未使用）
     * @param globalConfiguration 全局配置（未使用）
     * @return WriteCellData<?> 包含图片字节数组的 Excel 数据单元
     */
    @Override
    public WriteCellData<?> convertToExcelData(String url, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) {
        // 如果 URL 为空，则使用默认图片 URL
        String finalUrl = (url == null || url.trim().isEmpty()) ? DEFAULT_IMAGE_URL : url;

        // 下载图片（如果失败，则返回默认图片）
        byte[] imageBytes = downloadImage(finalUrl);
        if (imageBytes == null) {
            imageBytes = downloadImage(DEFAULT_IMAGE_URL);
        }

        return new WriteCellData<>(imageBytes);
    }

    /**
     * 从指定的 URL 下载图片并转换为字节数组。
     *
     * @param imageUrl 图片的 URL 地址
     * @return 图片的字节数组，如果下载失败则返回 null
     */
    private byte[] downloadImage(String imageUrl) {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            return IoUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("图片下载失败：{}，使用默认图片替代", imageUrl, e);
        }
        // 失败时返回 null
        return null;
    }
}
