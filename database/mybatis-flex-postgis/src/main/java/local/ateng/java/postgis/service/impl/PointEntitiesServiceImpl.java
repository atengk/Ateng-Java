package local.ateng.java.postgis.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import local.ateng.java.postgis.entity.PointEntities;
import local.ateng.java.postgis.mapper.PointEntitiesMapper;
import local.ateng.java.postgis.service.PointEntitiesService;
import org.springframework.stereotype.Service;

/**
 *  服务层实现。
 *
 * @author ATeng
 * @since 2025-04-21
 */
@Service
public class PointEntitiesServiceImpl extends ServiceImpl<PointEntitiesMapper, PointEntities>  implements PointEntitiesService{

}
