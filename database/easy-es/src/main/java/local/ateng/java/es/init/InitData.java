package local.ateng.java.es.init;

import com.github.javafaker.Faker;
import local.ateng.java.es.entity.MyUser;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {
    List<MyUser> list;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        List<MyUser> userList = new ArrayList();
        for (int i = 1; i <= 1000; i++) {
            MyUser user = new MyUser();
            user.setName(faker.name().fullName());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setPhoneNumber(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            user.setIpaddress(faker.internet().ipV4Address());
            user.setCompany(faker.company().name());
            user.setScore(BigDecimal.valueOf(faker.number().randomDouble(2, 0, 100)));
            user.setBirthday(faker.date().birthday());
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setAddress(faker.address().fullAddress());
            user.setLocation(faker.address().latitude() + "," + faker.address().longitude());
            user.setParagraph(faker.lorem().paragraph());
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list = userList;
    }

}
