package local.ateng.java.hutool;

import cn.hutool.core.io.file.FileReader;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 类的模板注释
 *
 * @author 孔余
 * @since 2024-01-17 21:38
 */
public class FileReaderTests {
    @Test
    void test01() throws IOException {
        FileReader fileReader = new FileReader("D:\\Temp\\202401\\my_user.sql");
        BufferedReader reader = fileReader.getReader();
        String s1 = reader.readLine();
        String s2 = reader.readLine();
        System.out.println(s1);
        System.out.println(s2);
    }
}
