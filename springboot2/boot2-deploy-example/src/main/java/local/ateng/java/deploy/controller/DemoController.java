package local.ateng.java.deploy.controller;

import io.github.atengk.service.AtengService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {
    private final AtengService atengService;

    @GetMapping("/getEnv")
    public Map<String, String> getEnv() {
        Map<String, String> env = atengService.getEnv();
        return env;
    }

    @GetMapping("/hello")
    public String hello() {
        return atengService.hello();
    }


}