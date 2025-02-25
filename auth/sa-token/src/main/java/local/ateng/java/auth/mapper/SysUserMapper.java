package local.ateng.java.auth.mapper;

import com.mybatisflex.core.BaseMapper;
import local.ateng.java.auth.entity.SysUser;

import java.util.List;

/**
 * 存储用户的基本信息 映射层。
 *
 * @author 孔余
 * @since 1.0.0
 */
public interface SysUserMapper extends BaseMapper<SysUser> {
    List<SysUser> selectAllUsers();
}
