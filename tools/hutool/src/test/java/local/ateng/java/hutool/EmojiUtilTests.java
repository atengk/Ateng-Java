package local.ateng.java.hutool;

import cn.hutool.extra.emoji.EmojiUtil;
import org.junit.jupiter.api.Test;

/**
 * 考虑到MySQL等数据库中普通的UTF8编码并不支持Emoji（只有utf8mb4支持），
 * 因此对于数据中的Emoji字符进行处理（转换、清除）变成一项必要工作。因此Hutool基于emoji-java库提供了Emoji工具实现。
 * https://www.hutool.cn/docs/#/extra/emoji/Emoji%E5%B7%A5%E5%85%B7-EmojiUtil
 *
 * @author 孔余
 * @since 2024-01-17 18:07
 */
public class EmojiUtilTests {
    // 转义Emoji字符
    @Test
    void test01() {
        String alias = EmojiUtil.toAlias("😄");//:smile:
        System.out.println(alias);
    }

    // 将转义的别名转为Emoji字符
    @Test
    void test02() {
        String emoji = EmojiUtil.toUnicode(":smile:");//😄
        System.out.println(emoji);
    }

    // 将字符串中的Unicode Emoji字符转换为HTML表现形式
    @Test
    void test03() {
        String alias = EmojiUtil.toHtml("😄");//&#128102;
        System.out.println(alias);
    }
}
