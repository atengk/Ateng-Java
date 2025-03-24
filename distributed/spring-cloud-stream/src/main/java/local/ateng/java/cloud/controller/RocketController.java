package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.entity.MyUser;
import local.ateng.java.cloud.service.RocketProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rocket")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RocketController {
    private final RocketProducerService rocketProducerService;

    @GetMapping("/send")
    public void send(String message) {
        rocketProducerService.sendMessage(message);
    }

    @PostMapping("/send-user")
    public void sendUser(@RequestBody MyUser message) {
        rocketProducerService.sendMessage(message);
    }

    @PostMapping("/send-user-param")
    public void sendUserParm(@RequestBody MyUser message) {
        rocketProducerService.sendMessageParam(message);
    }

}
