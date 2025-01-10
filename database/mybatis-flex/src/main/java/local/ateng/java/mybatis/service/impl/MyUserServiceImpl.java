package local.ateng.java.mybatis.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.mapper.MyUserMapper;
import local.ateng.java.mybatis.service.MyUserService;
import org.springframework.stereotype.Service;

/**
 * 用户信息表，存储用户的基本信息 服务层实现。
 *
 * @author 孔余
 * @since 1.0.0
 */
@Service
public class MyUserServiceImpl extends ServiceImpl<MyUserMapper, MyUser>  implements MyUserService{

}
