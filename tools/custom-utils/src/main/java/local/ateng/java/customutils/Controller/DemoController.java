package local.ateng.java.customutils.Controller;

import local.ateng.java.customutils.utils.SpringUtil;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/test")
    public String test() {
        Environment environment = SpringUtil.getEnvironment();
        System.out.println(environment);
        String[] beanDefinitionNames = SpringUtil.getBeanDefinitionNames();
        System.out.println(Arrays.asList(beanDefinitionNames));
        return "test";
    }

}
