package local.ateng.java.hutool;

import cn.hutool.core.util.ZipUtil;
import org.junit.jupiter.api.Test;

public class ZipUtils {

    /**
     * 将目录下的所有文件打包到指定目录（注意打包目录不要在源目录）
     */
    @Test
    void test01() {
        ZipUtil.zip("C:\\Users\\admin\\Downloads\\","D:\\Temp\\202410\\20241102\\20241102\\111.zip");
    }

    /**
     * 将目录打包到指定目录（注意打包目录不要在源目录）
     */
    @Test
    void test02() {
        ZipUtil.zip("C:\\Users\\admin\\Downloads\\","D:\\Temp\\202410\\20241102\\20241102\\111.zip", true);
    }


}
