package local.ateng.java.cloud.contro;

import local.ateng.java.cloud.demo.entity.RemoteUser;
import local.ateng.java.cloud.demo.service.RemoteUserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @DubboReference
    private RemoteUserService remoteUserService;

    @GetMapping("/get")
    public RemoteUser sayHello() {
        return remoteUserService.getUser();
    }

}
