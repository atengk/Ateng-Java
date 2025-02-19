package local.ateng.java.lock4j.controller;

import local.ateng.java.lock4j.entity.MyUser;
import local.ateng.java.lock4j.service.LockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lock")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockController {

    private final LockService lockService;

    @GetMapping("/simple")
    public String simple() {
        lockService.simple();
        return "Task completed with distributed lock.";
    }

    @PostMapping("/customMethod")
    public String customMethod(@RequestBody MyUser myUser) {
        lockService.customMethod(myUser);
        return "Task completed with distributed lock.";
    }

    @GetMapping("/programmatic/{id}")
    public String programmaticLock(@PathVariable String id) {
        lockService.programmaticLock(id);
        return "Task completed with distributed lock.";
    }

    @PostMapping("/limit")
    public String limit(@RequestBody MyUser myUser) {
        lockService.limit(myUser);
        return "Task completed with distributed lock.";
    }

}

