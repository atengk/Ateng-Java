package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SpringTests {

    @Test
    void list() {
        Environment environment = SpringUtil.getEnvironment();
        System.out.println(environment);
    }


}
