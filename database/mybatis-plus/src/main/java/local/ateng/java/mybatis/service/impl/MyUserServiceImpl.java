package local.ateng.java.mybatis.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    void test() {
        Page<MyUser> myUserPage = new Page<>();
        IPage<MyUser> page = this.page(null);
    }
}
