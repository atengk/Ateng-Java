package local.ateng.java.hutool;

import cn.hutool.core.util.CoordinateUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * CoordinateUtil
 * 坐标系转换相关工具类，主流坐标系包括：
 * WGS84坐标系：即地球坐标系，中国外谷歌地图
 * GCJ02坐标系：即火星坐标系，高德、腾讯、阿里等使用
 * BD09坐标系：即百度坐标系，GCJ02坐标系经加密后的坐标系。百度、搜狗等使用
 * 坐标转换相关参考: https://tool.lu/coordinate/
 * 参考：https://github.com/JourWon/coordinate-transform
 *
 * @author 孔余
 * @since 2024-01-15 16:07
 */
public class CoordinateUtilTests {

    // WGS84 转换为 火星坐标系 (GCJ-02)
    @Test
    void test1() {
        /**
         * 百度坐标系 (BD-09) 与 火星坐标系 (GCJ-02)的转换 即 百度 转 谷歌、高德: bd09ToGcj02(double lng, double lat)
         * 百度坐标系 (BD-09) 与 WGS84 的转换: bd09toWgs84(double lng, double lat)
         * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换: gcj02ToBd09(double lng, double lat)
         * 火星坐标系 (GCJ-02) 转换为 WGS84: gcj02ToWgs84(double lng, double lat)
         * 墨卡托投影 转为 WGS84 坐标: mercatorToWgs84(double mercatorX, double mercatorY)
         * 判断坐标是否在国外，火星坐标系 (GCJ-02)只对国内有效，国外无需转换: outOfChina(double lng, double lat)
         * WGS84 坐标转为 百度坐标系 (BD-09) 坐标: wgs84ToBd09(double lng, double lat)
         * WGS84 转换为 火星坐标系 (GCJ-02): wgs84ToGcj02(double lng, double lat)
         * WGS84 坐标转为 墨卡托投影: wgs84ToMercator(double lng, double lat)
         */
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.wgs84ToGcj02(106.550483, 29.563707);
        double lng = coordinate.getLng();
        double lat = coordinate.getLat();
        System.out.println(Arrays.asList(lng, lat));
    }

    // 火星坐标系 (GCJ-02) 转换为 WGS84
    @Test
    void test1_2() {
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.gcj02ToWgs84(106.308909,29.607348);
        double lng = coordinate.getLng();
        double lat = coordinate.getLat();
        System.out.println(Arrays.asList(lng, lat));
    }

    // 当前坐标偏移指定坐标
    @Test
    void test2() {
        /**
         * lng - 经度值
         * lat - 纬度值
         */
        CoordinateUtil.Coordinate coordinate = CoordinateUtil.wgs84ToGcj02(106.550483, 29.563707);
        double lng = coordinate.getLng();
        double lat = coordinate.getLat();
        // 经纬度各偏移 0.001
        coordinate.offset(new CoordinateUtil.Coordinate(0.001, 0.001));
        System.out.println(Arrays.asList(lng, lat));
        System.out.println(Arrays.asList(coordinate.getLng(), coordinate.getLat()));
    }

}
