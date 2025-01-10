package local.ateng.java.mybatis.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.mybatis.entity.MyOrder;
import local.ateng.java.mybatis.mapper.MyOrderMapper;
import local.ateng.java.mybatis.service.MyOrderService;
import org.springframework.stereotype.Service;

/**
 * 订单信息表，存储用户的订单数据 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class MyOrderServiceImpl extends ServiceImpl<MyOrderMapper, MyOrder>  implements MyOrderService{

}
