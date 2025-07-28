package local.ateng.java.mybatisjdk8;

import com.alibaba.fastjson2.*;
import com.alibaba.fastjson2.filter.Filter;
import local.ateng.java.mybatisjdk8.entity.MyData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

public class Fastjson2Tests {
    @Test
    public void jsonObject() {
        Filter autoTypeFilter = JSONReader.autoTypeFilter(
                "local.ateng.java."
        );

        JSONReader.Context context = JSONFactory.createReadContext(
                autoTypeFilter,
                JSONReader.Feature.FieldBased
        );

        String json1 = "{\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"id\":0,\"name\":\"test0\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}";
        Object obj1 = JSON.parseObject(json1, Object.class, context);
        System.out.println(obj1.getClass());

        String json2 = "{\"id\":0,\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"name\":\"test0\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}";
        Object obj2 = JSON.parseObject(json2, Object.class, context);
        System.out.println(obj2.getClass());
    }

    @Test
    public void jsonObjectB() {
        Filter autoTypeFilter = JSONReader.autoTypeFilter(
                "local.ateng.java."
        );

        JSONReader.Context context = JSONFactory.createReadContext(
                autoTypeFilter,
                JSONReader.Feature.UseDefaultConstructorAsPossible,
                JSONReader.Feature.UseNativeObject,
                JSONReader.Feature.FieldBased
        );

        MyData myData = new MyData();
        myData.setId(1L);
        myData.setName("test0");
        myData.setAddress("重庆市");
        myData.setDateTime(LocalDateTime.now());

        byte[] bytes = JSONB.toBytes(myData, JSONWriter.Feature.WriteClassName);
        Object obj = JSONB.parseObject(bytes, Object.class, context);
        System.out.println(obj.getClass());

        String jsonString = JSON.toJSONString(myData, JSONWriter.Feature.WriteClassName);
        System.out.println(jsonString);

        String json = "{\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"id\":0,\"name\":\"test0\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}";
        Object obj2 = JSON.parseObject(json, Object.class, context);
        System.out.println(obj2.getClass());

    }



    @Test
    public void jsonArray() {
        Filter autoTypeFilter = JSONReader.autoTypeFilter(
                "com.ateng.java."
        );

        JSONReader.Context context = JSONFactory.createReadContext(
                autoTypeFilter,
                JSONReader.Feature.SupportAutoType
        );

        String json = "[{\"@type\":\"local.ateng.java.mybatisjdk8.entity.MyData\",\"id\":0,\"name\":\"test0\",\"address\":\"重庆市0\",\"dateTime\":\"2025-07-27 21:14:54.599\"}]";
        List list = JSON.parseObject(json, List.class, context);
        System.out.println(list.get(0).getClass());
    }
}
