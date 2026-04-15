package local.ateng.java.hutool;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.io.unit.DataUnit;
import org.junit.jupiter.api.Test;

/**
 * DataSizeUtil 常见使用示例
 *
 * @author Ateng
 * @since 2026-04-15
 */
public class DataSizeUtilTests {

    /**
     * 将字节数格式化为可读大小
     */
    @Test
    public void test1() {
        long size = 1024L * 1024L * 15L;
        String result = DataSizeUtil.format(size);
        System.out.println(result);
    }

    /**
     * 指定单位进行格式化
     */
    @Test
    public void test3() {
        Long size = 1024L * 1024L * 15L;
        String result = DataSizeUtil.format(size, DataUnit.MEGABYTES);
        System.out.println(result);
    }

    /**
     * 解析字符串为字节数
     */
    @Test
    public void test4() {
        long size = DataSizeUtil.parse("12KB");
        System.out.println(size);
    }

    /**
     * 解析更大的数据单位字符串
     */
    @Test
    public void test5() {
        long size = DataSizeUtil.parse("5MB");
        System.out.println(size);
    }

}