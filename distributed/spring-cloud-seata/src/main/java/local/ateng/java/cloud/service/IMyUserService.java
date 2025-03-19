package local.ateng.java.cloud.service;

import local.ateng.java.cloud.entity.MyUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
public interface IMyUserService extends IService<MyUser> {

    void saveUser(MyUser myUser);

}
