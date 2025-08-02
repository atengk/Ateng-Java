package local.ateng.java.redisjdk8;


import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import local.ateng.java.redisjdk8.service.RedissonService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisServiceTests {
    private final RedissonService redissonService;

    @Test
    void set() {
        List<UserInfoEntity> list = new InitData().getList();
        redissonService.set("my:user", list.get(0));
        Map<String, UserInfoEntity> map = new HashMap<>();
        map.put("test", list.get(0));
        redissonService.set("my:userMap", map);
        redissonService.set("my:userList", list);
    }

    @Test
    void get() {
        UserInfoEntity userInfoEntity = redissonService.get("my:user",new TypeReference<UserInfoEntity>(){});
        System.out.println(userInfoEntity.getCity());
    }

    @Test
    void getList() {
        List<UserInfoEntity> list = redissonService.get("my:userList", new TypeReference<List<UserInfoEntity>>() {});
        System.out.println(list.get(0).getClass());
    }

    @Test
    void getMap() {
        Map<String, UserInfoEntity> map = redissonService.get("my:userMap", new TypeReference<Map<String, UserInfoEntity>>() {});
        System.out.println(map.get("test").getClass());
    }



}
