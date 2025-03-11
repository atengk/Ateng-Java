package local.ateng.java.cloud.service.impl;

import local.ateng.java.cloud.demo.entity.RemoteUser;
import local.ateng.java.cloud.demo.service.RemoteUserService;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;

@DubboService  // Dubbo 服务注解，暴露服务
public class RemoteUserServiceImpl implements RemoteUserService {
    @Override
    public RemoteUser getUser() {
        return RemoteUser.builder()
                .id(1L)
                .username("阿腾")
                .age(25)
                .createTime(LocalDateTime.now())
                .build();
    }
}
