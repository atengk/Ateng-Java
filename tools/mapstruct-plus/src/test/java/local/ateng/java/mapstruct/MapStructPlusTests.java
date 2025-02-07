package local.ateng.java.mapstruct;

import io.github.linpeilie.Converter;
import local.ateng.java.mapstruct.dto.MyUserDto;
import local.ateng.java.mapstruct.entity.MyUser;
import local.ateng.java.mapstruct.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MapStructPlusTests {
    private final Converter converter;

    @Test
    void test() {
        MyUser myUser = InitData.list.get(0);
        MyUserDto dto = converter.convert(myUser, MyUserDto.class);
        System.out.println(dto);
    }

}
