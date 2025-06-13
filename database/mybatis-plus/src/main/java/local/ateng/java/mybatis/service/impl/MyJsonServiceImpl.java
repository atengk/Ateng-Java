package local.ateng.java.mybatis.service.impl;

import local.ateng.java.mybatis.entity.MyJson;
import local.ateng.java.mybatis.mapper.MyJsonMapper;
import local.ateng.java.mybatis.service.IMyJsonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Json表 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2025-06-13
 */
@Service
public class MyJsonServiceImpl extends ServiceImpl<MyJsonMapper, MyJson> implements IMyJsonService {

}
