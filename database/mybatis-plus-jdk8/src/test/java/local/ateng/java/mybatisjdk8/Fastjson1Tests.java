package local.ateng.java.mybatisjdk8;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import local.ateng.java.mybatisjdk8.entity.MyData;
import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.enums.StatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

public class Fastjson1Tests {

    @Test
    public void jsonObject() {
        String json = "{\"id\":0,\"name2\":\"test0\",\"name\":\"test0\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}";
        Object obj = JSON.parseObject(json, Object.class, Feature.SupportAutoType);
        System.out.println(obj.getClass());
    }

    @Test
    public void jsonArray() {
        String json = "[{\"id\":0,\"name\":\"test0\",\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}]";
        List<MyData> list = JSON.parseObject(json, List.class, Feature.SupportAutoType);
        System.out.println(list.get(0).getClass());
        System.out.println(list.get(0).getAddress());
    }

    @Test
    public void enum01() {
        String json = "{\"name\":\"阿腾\",\"status\":3}";
        Project project = JSON.parseObject(json, Project.class);
        System.out.println(project);
    }

    @Test
    public void enum02() {
        String json = "{\"name\":\"阿腾\"}";
        Project project = JSON.parseObject(json, Project.class);
        System.out.println(project);
    }

    @Test
    public void enum03() {
        Project project = Project.builder()
                .name("阿腾")
                .status(StatusEnum.OFFLINE)
                .build();
        System.out.println(JSON.toJSONString(project));
    }

    @Test
    public void enum04() {
        Project project = Project.builder()
                .name("阿腾")
                .status(null)
                .build();
        System.out.println(JSON.toJSONString(project));
    }

}
