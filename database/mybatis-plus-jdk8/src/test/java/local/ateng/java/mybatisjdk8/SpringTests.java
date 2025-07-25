package local.ateng.java.mybatisjdk8;

import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

}
