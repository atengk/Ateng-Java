package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.TypeReference;

public class Fastjson2ArrayMyDataTypeHandler<T> extends Fastjson2GenericTypeHandler<T> {

    public Fastjson2ArrayMyDataTypeHandler() {
        super(new TypeReference<T>() {
        });
    }

}
