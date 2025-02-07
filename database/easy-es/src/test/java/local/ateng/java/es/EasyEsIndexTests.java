package local.ateng.java.es;

import local.ateng.java.es.entity.MyUser;
import local.ateng.java.es.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.easyes.annotation.rely.FieldType;
import org.dromara.easyes.core.kernel.EsWrappers;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EasyEsIndexTests {
    private final MyUserMapper myUserMapper;

    @Test
    public void exists() {
        // 是否存在索引
        Boolean result = myUserMapper.existsIndex("my_user");
        System.out.println(result);
    }

    @Test
    public void create() {
        // 根据实体及自定义注解一键创建索引
        Boolean result = myUserMapper.createIndex();
        System.out.println(result);
    }

    @Test
    public void getIndex() {
        // 获取索引信息
        GetIndexResponse index = myUserMapper.getIndex();
        System.out.println(index.getSettings());
        System.out.println(index.getMappings().get("my_user").getSourceAsMap());
    }

    @Test
    public void update() {
        // 更新索引
        Boolean result = EsWrappers.lambdaChainIndex(myUserMapper)
                .indexName("my_user")
                .mapping(MyUser::getAddress, FieldType.KEYWORD_TEXT)
                .updateIndex();
        System.out.println(result);
    }

    @Test
    public void delete() {
        // 删除索引
        Boolean result = myUserMapper.deleteIndex("my_user");
        System.out.println(result);
    }

}
