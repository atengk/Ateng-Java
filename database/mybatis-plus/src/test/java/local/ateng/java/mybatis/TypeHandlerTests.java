package local.ateng.java.mybatis;

import local.ateng.java.mybatis.entity.MyData;
import local.ateng.java.mybatis.entity.MyDataList;
import local.ateng.java.mybatis.entity.Project;
import local.ateng.java.mybatis.service.IProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class TypeHandlerTests {
    @Autowired
    private IProjectService projectService;

    @Test
    void list() {
        List<Project> list = projectService.list();
        System.out.println(list);
    }

    @Test
    void saveJson() {
        MyData myData = new MyData();
        myData.setId(1L);
        myData.setName("test");
        myData.setAddress("重庆市");
        myData.setDateTime(LocalDateTime.now());
        myData.setDate(new Date());

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
            myData.setDate(new Date());
            list.add(myData);
        }

        Project project = new Project();
        project.setName("json");
        project.setJsonArray(list);
//        project.setJsonArray(new MyDataList(list));
        projectService.save(project);
    }

}
