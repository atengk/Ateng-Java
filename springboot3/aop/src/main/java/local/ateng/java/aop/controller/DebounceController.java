package local.ateng.java.aop.controller;

import local.ateng.java.aop.annotation.Debounce;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/debounce")
public class DebounceController {

    @GetMapping("/demo")
    @Debounce(interval = 1000, timeUnit = TimeUnit.MILLISECONDS, message = "操作频繁，请稍候再试")
    public String demo() {
        return "ok";
    }

}
