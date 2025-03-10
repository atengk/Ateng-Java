package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.config.AtengConfig;
import local.ateng.java.cloud.config.MyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ConfigController {
    private final AtengConfig atengConfig;
    private final MyConfig myConfig;

    @GetMapping("/get")
    public Map<String, String> getConfig() {
        return new LinkedHashMap<>() {{
            put("ateng-config", atengConfig.toString());
            put("my-config", myConfig.toString());
        }};
    }

}
