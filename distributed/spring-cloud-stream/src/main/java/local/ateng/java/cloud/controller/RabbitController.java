//package local.ateng.java.cloud.controller;
//
//import local.ateng.java.cloud.entity.MyUser;
//import local.ateng.java.cloud.service.RabbitProducerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/rabbit")
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//public class RabbitController {
//    private final RabbitProducerService rabbitProducerService;
//
//    @PostMapping("/send")
//    public void send(@RequestBody MyUser message) {
//        rabbitProducerService.sendMessage(message);
//    }
//
//    @PostMapping("/send-delay")
//    public void sendDelay(@RequestBody MyUser message) {
//        rabbitProducerService.sendMessageDelay(message);
//    }
//
//    @PostMapping("/send-batch")
//    public void sendBatch(@RequestBody MyUser message) {
//        rabbitProducerService.sendMessageBatch(message);
//    }
//
//    @PostMapping("/send-manual")
//    public void sendManual(@RequestBody MyUser message) {
//        rabbitProducerService.sendMessageManual(message);
//    }
//
//    @PostMapping("/send-two")
//    public void sendTwo(@RequestBody MyUser message) {
//        rabbitProducerService.sendMessageTwo(message);
//    }
//
//    @GetMapping("/send-ateng")
//    public void sendAteng(String message) {
//        rabbitProducerService.sendMessageAteng(message);
//    }
//
//}