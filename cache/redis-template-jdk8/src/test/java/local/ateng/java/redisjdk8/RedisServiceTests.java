package local.ateng.java.redisjdk8;

import com.fasterxml.jackson.core.type.TypeReference;
import local.ateng.java.redisjdk8.entity.UserInfoEntity;
import local.ateng.java.redisjdk8.init.InitData;
import local.ateng.java.redisjdk8.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedisServiceTests {
    private final RedisService redisService;

    @Test
    void set() {
        List<UserInfoEntity> list = new InitData().getList();
        redisService.set("my:user", list.get(0));
        redisService.set("my:userList", list);
    }

    @Test
    void get() {
        UserInfoEntity user = redisService.get("my:user", UserInfoEntity.class);
        List<UserInfoEntity> userList = redisService.get("my:userList", List.class);
        System.out.println(user.getClass());
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void getTypeReference() {
        List<UserInfoEntity> userList = redisService.get("my:userList", new TypeReference<List<UserInfoEntity>>() {});
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void getList() {
        List<UserInfoEntity> userList = redisService.getList("my:userList", UserInfoEntity.class);
        System.out.println(userList.get(0).getClass());
    }

    @Test
    void multiGet() {
        List<UserInfoEntity> entityList = redisService.multiGet(Arrays.asList("my:user", "my:user2"), UserInfoEntity.class);
        System.out.println(entityList.get(0).getClass());
    }

    @Test
    void hSet() {
        redisService.hSet("my:userMap", "a", new InitData().getList().get(0));
    }

    @Test
    void tryLock() {
        boolean result = redisService.tryLock("my", "v", 1, TimeUnit.HOURS);
        System.out.println(result);
    }

    @Test
    void releaseLock() {
        boolean result = redisService.releaseLock("my", "v");
        System.out.println(result);
    }

    @Test
    void renewLock() {
        boolean result = redisService.renewLock("my", "v", 1, TimeUnit.HOURS);
        System.out.println(result);
    }


}
