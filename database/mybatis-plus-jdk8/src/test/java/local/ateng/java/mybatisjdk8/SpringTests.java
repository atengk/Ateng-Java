package local.ateng.java.mybatisjdk8;

import cn.hutool.core.codec.Base64;
import local.ateng.java.mybatisjdk8.entity.MyData;
import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SpringTests {
    private final IProjectService projectService;

    @Test
    void list() {
        List<Project> list = projectService.list();
        System.out.println(list);
    }

    @Test
    void saveIp() {
        Project project = new Project();
        project.setName("test");
        project.setIpAddress("192.168.1.12");
        projectService.save(project);
    }

    @Test
    void saveBase64() {
        Project project = new Project();
        project.setName("test");
        project.setBinaryData(Base64.encode("你好，我是阿腾！"));
        projectService.save(project);
    }

    @Test
    void saveGeometry() {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Point point = factory.createPoint(new Coordinate(106.55, 29.56));
        Project project = new Project();
        project.setName("test");
        project.setLocation(point);
        projectService.save(project);
    }

    @Test
    void saveJson() {
        MyData myData = new MyData();
        myData.setId(1L);
        myData.setName("test");
        myData.setAddress("重庆市");
        myData.setDateTime(LocalDateTime.now());

        Project project = new Project();
        project.setName("json");
        project.setJsonObject(myData);
        projectService.save(project);
    }

    @Test
    void saveJsonArray() {
        List<MyData> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            MyData myData = new MyData();
            myData.setId((long) i);
            myData.setName("test" + i);
            myData.setAddress("重庆市" + i);
            myData.setDateTime(LocalDateTime.now());
            list.add(myData);
        }

        Project project = new Project();
        project.setName("json");
        project.setJsonArray(list);
        projectService.save(project);
    }

}
