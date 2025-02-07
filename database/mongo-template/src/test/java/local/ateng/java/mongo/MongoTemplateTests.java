package local.ateng.java.mongo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MongoTemplateTests {
    private final MongoTemplate mongoTemplate;

    // 新增数据
    @Test
    void saveUser() {
        Collection<MyUser> list = mongoTemplate.insertAll(InitData.list);
        System.out.println(list);
    }

    // 查询数据列表
    @Test
    void getAll() {
        List<MyUser> users = mongoTemplate.findAll(MyUser.class);
        users.forEach(System.out::println);
    }

    // 根据ID查询
    @Test
    void getById() {
        MyUser myUser = mongoTemplate.findById("67a5be3d2509c17b98a797ac", MyUser.class);
        System.out.println(myUser);
    }

    // 条件查询
    @Test
    void getQuery() {
        Query query = new Query(Criteria.where("province").is("重庆市").and("age").gte(25));
        List<MyUser> users = mongoTemplate.find(query, MyUser.class);
        users.forEach(System.out::println);
    }

    // 模糊查询
    @Test
    void findLikeName() {
        String province = "重庆";
        String regex = String.format("%s%s%s", "^.*", province, ".*$");//采用正则表达式进行匹配
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("province").regex(pattern));
        List<MyUser> users = mongoTemplate.find(query, MyUser.class);
        users.forEach(System.out::println);
    }

    // 分页查询
    @Test
    void findPage() {
        String province = "重庆";
        int pageNo = 1; // 当前页
        int pageSize = 10; // 每页的大小

        Query query = new Query(); //条件构建部分
        String regex = String.format("%s%s%s", "^.*", province, ".*$");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("province").regex(pattern));
        int totalCount = (int) mongoTemplate.count(query, MyUser.class); //查询记录数
        //其中的skip表示跳过的记录数，当前页为1则跳过0条，为2则跳过10条，（也就是跳过第一页的10条数据）
        List<MyUser> users = mongoTemplate.find(query.skip((pageNo - 1) * pageSize).limit(pageSize), MyUser.class);//分页查询
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", users);
        map.put("total", totalCount);
        System.out.println(map);
    }

    // 修改数据
    @Test
    void updateUser() {
        Query query = new Query(Criteria.where("_id").is("67a5be3d2509c17b98a797dd"));
        Update update = new Update().set("age", 26);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, MyUser.class);
        System.out.println(updateResult);
    }

    // 删除数据
    @Test
    void delete() {
        Query query = new Query(Criteria.where("_id").is("67a5be3d2509c17b98a797dd"));
        DeleteResult result = mongoTemplate.remove(query, MyUser.class);
        System.out.println(result);
    }

}
