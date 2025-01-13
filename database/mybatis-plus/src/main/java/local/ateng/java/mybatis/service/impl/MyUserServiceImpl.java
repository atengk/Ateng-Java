package local.ateng.java.mybatis.service.impl;

import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.mapper.MyUserMapper;
import local.ateng.java.mybatis.service.IMyUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息表，存储用户的基本信息 服务实现类
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
@Service
public class MyUserServiceImpl extends ServiceImpl<MyUserMapper, MyUser> implements IMyUserService {

}
