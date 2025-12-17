package local.ateng.java.customutils.controller;

import local.ateng.java.customutils.ToolsCustomUtilsApplication;
import local.ateng.java.customutils.enums.BaseEnum;
import local.ateng.java.customutils.event.UserRegisterEvent;
import local.ateng.java.customutils.utils.EnumUtil;
import local.ateng.java.customutils.utils.SpringUtil;
import local.ateng.java.customutils.utils.SystemUtil;
import local.ateng.java.customutils.utils.ValidateUtil;
import lombok.Data;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.*;

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

    @GetMapping("/getMainApplicationPackage")
    public String getMainApplicationPackage() {
        System.out.println(SpringUtil.getMainApplicationPackage());
        return "ok";
    }

    @GetMapping("/scanAllBaseEnums")
    public void scanAllBaseEnums(String basePackage) {
        Set<Class<? extends BaseEnum<?, ?>>> enums1 = EnumUtil.scanAllBaseEnums();
        Set<Class<? extends BaseEnum<?, ?>>> enums2 = EnumUtil.scanAllBaseEnums(basePackage);
        Set<Class<? extends BaseEnum<?, ?>>> enums3 = EnumUtil.scanAllBaseEnums(basePackage, basePackage);
        System.out.println(enums2);
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


    @GetMapping("/getJvmUptime")
    public Long getJvmUptime() {
        return SystemUtil.getJvmUptime();
    }

    @GetMapping("/net")
    public void SystemUtil() {
        System.out.println("主机名: " + SystemUtil.getHostName());
        System.out.println("IPv4: " + SystemUtil.getLocalIpV4());
        System.out.println("IPv6: " + SystemUtil.getLocalIpV6());
        System.out.println("MAC地址: " + SystemUtil.getMacAddress(null));
        System.out.println("全部网卡信息:\n" + SystemUtil.getAllNetworkInfo());
    }


    @GetMapping("/validateFirst")
    public void validateBean() {
        User user = new User();
        String str = ValidateUtil.validateFirst(user);
        String err = ValidateUtil.validatePropertyFirst(user, "name");
        System.out.println(str);
        System.out.println(err);
    }

    @GetMapping("/validateAll")
    public void validateBeanAll() {
        User user = new User();
        List<String> str = ValidateUtil.validateAll(user);
        System.out.println(str);
    }

    @GetMapping("/validateThrow")
    public void validateThrow() {
        User user = new User();

        ValidateUtil.validateThrow(user, msg -> new RuntimeException(msg));
    }


    @Data
    static class User {
        @NotNull(message = "id不能为空")
        private Long id;
        @NotNull(message = "名称不能为空")
        private String name;
        @NotNull(message = "密码不能为空")
        private String password;
    }

}
