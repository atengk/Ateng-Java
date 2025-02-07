package local.ateng.java.es;

import local.ateng.java.es.entity.MyUser;
import local.ateng.java.es.init.InitData;
import local.ateng.java.es.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.easyes.core.biz.EsPageInfo;
import org.dromara.easyes.core.biz.SAPageInfo;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryChainWrapper;
import org.dromara.easyes.core.kernel.EsWrappers;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EasyEsDataTests {
    private final MyUserMapper myUserMapper;

    @Test
    public void insertBatch() {
        // 批量插入数据
        List<MyUser> list = new InitData().getList();
        Integer result = myUserMapper.insertBatch(list);
        System.out.println(result);
    }

    @Test
    public void one() {
        // 查询数据
        MyUser myUser = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getProvince, "重庆市")
                .limit(1)
                .one();
        System.out.println(myUser);
    }

    @Test
    public void list() {
        // 查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getProvince, "重庆市")
                .list();
        System.out.println(list);
    }

    @Test
    public void listByTime() {
        // 根据时间范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .between(MyUser::getBirthday, "1990-01-01", "2000-01-01")
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }

    @Test
    public void listByIP() {
        // 根据IP范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getIpaddress, "10.0.0.0/8")
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }

    @Test
    public void listByGeo() {
        // 根据经纬度范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .geoDistance(MyUser::getLocation, 1000.0, DistanceUnit.KILOMETERS, new GeoPoint(39.92, 116.44))
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }

    @Test
    public void page() {
        // 浅分页
        // 分页查询，适用于查询数据量少于1万的情况
        EsPageInfo<MyUser> pageInfo = EsWrappers.lambdaChainQuery(myUserMapper)
                .match(MyUser::getProvince, "重庆")
                .page(1, 10);
        System.out.println(pageInfo);
    }

    @Test
    public void searchAfter() {
        // 分页查询
        // 使用searchAfter必须指定排序,若没有排序不仅会报错,而且对跳页也不友好. 需要保持searchAfter排序唯一,不然会导致分页失效,推荐使用id,uuid等进行排序.
        int pageSize = 10;
        LambdaEsQueryChainWrapper<MyUser> wrapper = EsWrappers.lambdaChainQuery(myUserMapper);
        wrapper.size(pageSize);
        // 必须指定一种排序规则,且排序字段值必须唯一 此处我选择用id进行排序 实际可根据业务场景自由指定,不推荐用创建时间,因为可能会相同
        wrapper.orderByDesc(MyUser::getId);
        // 第一页
        SAPageInfo<MyUser> saPageInfo = wrapper.searchAfterPage(null, 10);
        System.out.println(saPageInfo);
        // 获取下一页
        List<Object> nextSearchAfter = saPageInfo.getNextSearchAfter();
        SAPageInfo<MyUser> saNextPageInfo = wrapper.searchAfterPage(nextSearchAfter, pageSize);
        System.out.println(saNextPageInfo);
    }

}
