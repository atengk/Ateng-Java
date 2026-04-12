package local.ateng.java.mybatisjdk8.entity;

import java.util.List;

/**
 * MyData 列表包装
 *
 * @author Ateng
 * @since 2026-04-12
 */
public class MyDataList extends JsonWrapper<List<MyData>> {

    public MyDataList() {
    }

    public MyDataList(List<MyData> value) {
        super(value);
    }
}