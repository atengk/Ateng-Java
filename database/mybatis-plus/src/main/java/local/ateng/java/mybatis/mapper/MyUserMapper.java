package local.ateng.java.mybatis.mapper;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import local.ateng.java.mybatis.entity.MyUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 用户信息表，存储用户的基本信息 Mapper 接口
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
public interface MyUserMapper extends BaseMapper<MyUser> {

    List<MyUser> selectAllUsers();

    MyUser selectUserById(@Param("id") Long id);

    // 根据查询条件获取用户及其订单信息
    List<JSONObject> selectUsersWithOrders(@Param("orderId") Long orderId);

    // 分页查询
    IPage<JSONObject> selectUsersWithOrderPage(IPage page, @Param("city") String city);

    // 分页查询，传入wrapper
    IPage<JSONObject> selectUsersWithOrderPageWrapper(IPage page, @Param(Constants.WRAPPER) Wrapper wrapper);

    // 分页查询，传入wrapper、自定义查询条件
    IPage<JSONObject> selectUsersWithOrderPageWrapperQuery(IPage page, @Param(Constants.WRAPPER) Wrapper wrapper, @Param("query") Map<String, Object> query);
}
