package local.ateng.java.aop.controller;

import local.ateng.java.aop.service.TimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/timer")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TimerController {
    private final TimerService timerService;

    @GetMapping("/one")
    public void one() {
        timerService.one();
    }

    @GetMapping("/two")
    public void two() {
        timerService.two();
    }

}
