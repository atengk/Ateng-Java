package local.ateng.java.customutils.entity;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class Person {
    @JacksonXmlProperty(namespace="ateng")
    private String name;
    @JacksonXmlProperty(namespace="kongyu")
    private int age;
}
