package local.ateng.java.hutool;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

public class JsonUtilTests {
    @Test
    public void isTypeJSON() {
        boolean result = JSONUtil.isTypeJSON("axaw23123##!#!");
        System.out.println(result);
    }
}
