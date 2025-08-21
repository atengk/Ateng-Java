package local.ateng.java.mybatisjdk8;

import cn.hutool.core.util.RandomUtil;
import com.github.javafaker.Faker;
import local.ateng.java.mybatisjdk8.entity.MyData;
import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.enums.StatusEnum;
import local.ateng.java.mybatisjdk8.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyBatisPlusTests {
    private final IProjectService projectService;


    private static final Faker faker = new Faker(new Locale("zh-CN"));
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public static List<Project> generateProjects(int count) {
        List<Project> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Project p = new Project();
            //p.setUuid(UUID.randomUUID());
            p.setName(faker.company().name());
            p.setCode("PRJ-" + faker.number().digits(6));
            p.setDescription(faker.lorem().paragraph());
            p.setAmount(BigDecimal.valueOf(faker.number().randomDouble(2, 1000, 100000)));
            p.setScore(faker.number().randomDouble(2, 1, 10));
            p.setBalance(faker.number().numberBetween(10000L, 1_000_000L));
            p.setTags(generateTags());
            p.setPriority(faker.options().option("low", "medium", "high"));
            //p.setStatus(faker.number().numberBetween(0, 3));
            p.setIsActive(faker.bool().bool());
            p.setIsDeleted(faker.bool().bool());
            p.setVersion(faker.number().numberBetween(1, 10));
            p.setUserCount(faker.number().numberBetween(1, 1000));
            p.setBirthDate(LocalDate.now().minusYears(faker.number().numberBetween(20, 50)));
            p.setLastLogin(LocalTime.now().minusMinutes(faker.number().numberBetween(1, 1440)));
            p.setStartDate(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 100)));
            p.setEndDate(LocalDateTime.now().plusDays(faker.number().numberBetween(1, 100)));
            p.setRegion(faker.address().state());
            p.setFilePath("/upload/" + faker.file().fileName().replace(File.separator, "/"));
            p.setJsonObject(generateMyData());
            p.setJsonArray(Arrays.asList(generateMyData(), generateMyData(), generateMyData(), generateMyData(), generateMyData()));
            p.setLocation(generateRandomPoint());
            p.setIpAddress(RandomUtil.randomEle(new String[]{faker.internet().ipV6Address(), faker.internet().ipV4Address()}));
            p.setBinaryData(Base64.getEncoder().encodeToString(faker.lorem().paragraph().getBytes()));
            p.setCreatedAt(LocalDateTime.now().minusDays(faker.number().numberBetween(10, 100)));
            p.setUpdatedAt(LocalDateTime.now());

            list.add(p);
        }

        return list;
    }

    // 生成1~TAG_OPTIONS.size()个标签，逗号分隔
    public static String generateTags() {
        List<String> TAG_OPTIONS = Arrays.asList("tag1", "tag2", "tag3", "tag4");
        Collections.shuffle(TAG_OPTIONS);
        int count = new Random().nextInt(TAG_OPTIONS.size()) + 1; // 1~4个
        List<String> selected = TAG_OPTIONS.subList(0, count);
        return String.join(",", selected);
    }

    private static MyData generateMyData() {
        MyData data = new MyData();
        data.setId(faker.number().randomNumber());
        data.setName(faker.name().fullName());
        data.setAddress(faker.address().fullAddress());
        data.setScore(faker.number().randomDouble(2, 0, 100));
        data.setSalary(BigDecimal.valueOf(faker.number().randomDouble(2, 3000, 20000)));
        data.setDateTime(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 365)));
        return data;
    }

    private static Point generateRandomPoint() {
        String lat = faker.address().latitude();
        String lon = faker.address().longitude();
        return geometryFactory.createPoint(new Coordinate(Double.parseDouble(lon), Double.parseDouble(lat)));
    }


    @Test
    public void test() {
        // 生成数据
        for (int i = 0; i < 10000; i++) {
            try {
                List<Project> projects = generateProjects(100);
                projectService.saveBatch(projects);
            } catch (Exception e) {
            }
        }
    }

    @Test
    public void count() {
        long count = projectService.count();
        System.out.println(count);
    }

    @Test
    public void getOne() {
        Project project = projectService.getById(1);
        System.out.println(project);
    }

    @Test
    public void saveEnum() {
        Project project = Project.builder()
                .name("阿腾")
                .status(StatusEnum.OFFLINE)
                .build();
        projectService.save(project);
        System.out.println(project);
    }

}
