package local.ateng.java.customutils.controller;

import local.ateng.java.customutils.event.UserRegisterEvent;
import local.ateng.java.customutils.utils.SpringUtil;
import org.springframework.context.ApplicationContext;
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

    @GetMapping("/publishEvent")
    public String publishEvent() {
        SpringUtil.publishEvent(new UserRegisterEvent(this, "ateng"));
        SpringUtil.publishEvent("Hello, Spring Event!");
        SpringUtil.publishEvent("Hello, Ateng!");
        ApplicationContext context = SpringUtil.getApplicationContext();
        context.publishEvent(new UserRegisterEvent(this, "ateng"));
        return "ok";
    }

    @GetMapping("/getResourceReadString")
    public String getResourceReadString() {
        System.out.println(SpringUtil.getResourceReadString("application.yml"));
        return "ok";
    }

}
