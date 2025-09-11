package local.ateng.java.customutils.controller;

import local.ateng.java.customutils.ToolsCustomUtilsApplication;
import local.ateng.java.customutils.event.UserRegisterEvent;
import local.ateng.java.customutils.utils.EnumUtil;
import local.ateng.java.customutils.utils.SpringUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/getAllBaseEnumMap")
    public Map<String, List<Map<String, Object>>> getAllEnums() {
        return EnumUtil.getAllBaseEnumMap();
    }

    @GetMapping("/getLabelValueListByEnumName")
    public List<Map<String, Object>> getLabelValueListByEnumName(String name) throws ClassNotFoundException {
        String mainApplicationPackage = SpringUtil.getMainApplicationPackage(ToolsCustomUtilsApplication.class);
        System.out.println(mainApplicationPackage);
        return EnumUtil.getLabelValueListByEnumName(name);
    }

    @GetMapping("/buildUrl")
    public void buildUrl() {
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("id", 2002);
        uriVariables.put("name", "阿腾");

        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("page", 1);
        queryParams.put("size", 20);
        queryParams.put("name", "{name}");

        String url = SpringUtil.buildUrl("http://localhost:8080/api/user/{id}", queryParams, uriVariables);
        System.out.println(url);
    }

}
