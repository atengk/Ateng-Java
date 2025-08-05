package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.ZipUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class ZipTests {

    @Test
    void zip() throws IOException {
        ZipUtil.zip(Paths.get("D:\\Temp\\test"), Paths.get("D:\\Temp\\20250805.zip"));
    }

    @Test
    void zipList() throws IOException {
        ZipUtil.zip(Arrays.asList(
                Paths.get("D:\\Temp\\1.xlsx"),
                Paths.get("D:\\Temp\\1.xlsx")
        ), Paths.get("D:\\Temp\\20250805.sql.zip"));
    }

}
