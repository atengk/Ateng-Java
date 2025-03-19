package local.ateng.java.cloud.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import local.ateng.java.cloud.entity.MyUser;
import local.ateng.java.cloud.mapper.MyUserMapper;
import local.ateng.java.cloud.seata.entity.RemoteOrder;
import local.ateng.java.cloud.seata.service.RemoteOrderService;
import local.ateng.java.cloud.service.IMyUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyUserServiceImpl extends ServiceImpl<MyUserMapper, MyUser> implements IMyUserService {
    //private final FeignMyOrderService feignMyOrderService;
    @DubboReference
    private RemoteOrderService remoteOrderService;

    @Override
    @GlobalTransactional
    public void saveUser(MyUser myUser) {
        int num = new Random().nextInt(0, 2);
        // 当起模块写入
        this.save(myUser);
        // 其他远程模块写入，通过OpenFeign
        //feignMyOrderService.save(MyOrder.builder().userId(myUser.getId()).date(LocalDate.now()).totalAmount(new BigDecimal("1213.12")).build());
        // 其他远程模块写入，通过Dubbo
        remoteOrderService.save(RemoteOrder.builder().userId(myUser.getId()).date(LocalDate.now()).totalAmount(new BigDecimal("1213.12")).build());
        // 模拟异常
        if (num == 0) {
            throw new RuntimeException();
        }
    }
}
