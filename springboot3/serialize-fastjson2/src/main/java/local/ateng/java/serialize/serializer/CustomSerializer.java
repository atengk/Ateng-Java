package local.ateng.java.serialize.serializer;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

public class CustomSerializer implements ObjectWriter {

    @Override
    public void write(JSONWriter writer, Object object, Object fieldName, Type fieldType, long features) {
        writer.writeString(object + "~");
    }

}
