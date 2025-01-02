package local.ateng.java.hutool;

import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.Test;

/**
 * 在分布式环境中，唯一ID生成应用十分广泛，生成方法也多种多样，Hutool针对一些常用生成策略做了简单封装。
 * https://doc.hutool.cn/pages/IdUtil/#%E4%BB%8B%E7%BB%8D
 */
public class IdUtilTests {

    /**
     * 简化的UUID，去掉了横线
     */
    @Test
    public void test1() {
        String uuid = IdUtil.fastSimpleUUID();
        System.out.println(uuid);
    }

    /**
     * 随机UUID
     */
    @Test
    public void test2() {
        String uuid = IdUtil.fastUUID();
        System.out.println(uuid);
    }

}
