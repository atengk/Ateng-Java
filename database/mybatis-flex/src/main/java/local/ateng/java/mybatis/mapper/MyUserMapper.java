package local.ateng.java.mybatis.mapper;

import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.BaseMapper;
import local.ateng.java.mybatis.entity.MyUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户信息表，存储用户的基本信息 映射层。
 *
 * @author 孔余
 * @since 1.0.0
 */
public interface MyUserMapper extends BaseMapper<MyUser> {

    List<MyUser> selectAllUsers();

    MyUser selectUserById(@Param("id") Long id);

    // 根据查询条件获取用户及其订单信息
    List<JSONObject> selectUsersWithOrders(@Param("orderId") Long orderId);

}
