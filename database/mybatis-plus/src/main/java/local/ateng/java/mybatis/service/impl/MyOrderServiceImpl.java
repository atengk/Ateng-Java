package local.ateng.java.mybatis.service.impl;

import local.ateng.java.mybatis.entity.MyOrder;
import local.ateng.java.mybatis.mapper.MyOrderMapper;
import local.ateng.java.mybatis.service.IMyOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单信息表，存储用户的订单数据 服务实现类
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
@Service
public class MyOrderServiceImpl extends ServiceImpl<MyOrderMapper, MyOrder> implements IMyOrderService {

}
