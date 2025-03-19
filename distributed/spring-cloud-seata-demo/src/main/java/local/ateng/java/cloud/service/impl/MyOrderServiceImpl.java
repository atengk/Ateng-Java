package local.ateng.java.cloud.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import local.ateng.java.cloud.entity.MyOrder;
import local.ateng.java.cloud.mapper.MyOrderMapper;
import local.ateng.java.cloud.service.IMyOrderService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单信息表，存储用户的订单数据 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@Service
public class MyOrderServiceImpl extends ServiceImpl<MyOrderMapper, MyOrder> implements IMyOrderService {

}
