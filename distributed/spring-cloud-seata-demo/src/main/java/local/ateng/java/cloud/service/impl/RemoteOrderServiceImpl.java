package local.ateng.java.cloud.service.impl;

import local.ateng.java.cloud.entity.MyOrder;
import local.ateng.java.cloud.seata.entity.RemoteOrder;
import local.ateng.java.cloud.seata.service.RemoteOrderService;
import local.ateng.java.cloud.service.IMyOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RemoteOrderServiceImpl implements RemoteOrderService {
    private final IMyOrderService myOrderService;

    @Override
    public RemoteOrder save(RemoteOrder remoteOrder) {
        MyOrder myOrder = new MyOrder();
        BeanUtils.copyProperties(remoteOrder, myOrder);
        myOrderService.save(myOrder);
        BeanUtils.copyProperties(myOrder, remoteOrder);
        return remoteOrder;
    }

}
