//package local.ateng.java.cloud.controller;
//
//import local.ateng.java.cloud.entity.MyUser;
//import local.ateng.java.cloud.service.KafkaProducerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/kafka")
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//public class KafkaController {
//    private final KafkaProducerService kafkaProducerService;
//
//    @PostMapping("/send")
//    public void send(@RequestBody MyUser message) {
//        kafkaProducerService.sendMessage(message);
//    }
//
//    @PostMapping("/sendParam")
//    public void sendParam(@RequestBody MyUser message) {
//        kafkaProducerService.sendMessageParam(message);
//    }
//
//    @GetMapping("/send")
//    public void send(String message) {
//        kafkaProducerService.sendMessage(message);
//        //kafkaProducerService.sendMessageAndKey(message);
//    }
//
//    @PostMapping("/send-batch")
//    public void sendBatch(@RequestBody MyUser message) {
//        kafkaProducerService.sendMessageBatch(message);
//    }
//
//    @PostMapping("/send-manual")
//    public void sendManual(@RequestBody MyUser message) {
//        kafkaProducerService.sendMessageManual(message);
//    }
//
//    @PostMapping("/send-other")
//    public void sendOther(@RequestBody MyUser message) {
//        kafkaProducerService.sendMessageOther(message);
//    }
//
//}
