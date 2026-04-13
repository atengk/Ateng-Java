package local.ateng.java.mybatis;

import local.ateng.java.mybatis.entity.Project;
import local.ateng.java.mybatis.mapper.ProjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class ProjectMapperTests {

    @Autowired
    private ProjectMapper projectMapper;

    @Test
    void list() {
        List<Project> list = projectMapper.list();
        System.out.println(list);
    }

}
