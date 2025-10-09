package local.ateng.java.serialize.serializer;


import com.alibaba.fastjson2.filter.BeanContext;
import com.alibaba.fastjson2.filter.ContextValueFilter;

public class DefaultValueFilter implements ContextValueFilter {

    @Override
    public Object process(BeanContext context, Object object, String name, Object value) {
        if (value != null) {
            return value;
        }

        // 如果字段上有注解，就用注解里的值
        DefaultNullValue ann = context.getField().getAnnotation(DefaultNullValue.class);
        if (ann != null) {
            return ann.value();
        }

        // 没有注解就返回原来的 null
        return null;
    }
}
