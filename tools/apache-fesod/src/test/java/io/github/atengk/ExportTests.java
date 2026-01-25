package io.github.atengk;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import org.apache.fesod.sheet.FesodSheet;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExportTests {

    @Test
    void testExportSimple() {
        List<MyUser> list = InitData.getDataList();
        String fileName = "target/export_simple_users.xlsx";
        FesodSheet
                .write(fileName, MyUser.class)
                .sheet("用户列表")
                .doWrite(list);
    }

    @Test
    void testExportSimple2() {
        List<MyUser> list = InitData.getDataList();
        List<MyUser> collect = list.stream()
                .sorted(Comparator.comparing(MyUser::getCity))
                .collect(Collectors.toList());
        String fileName = "target/export_simple_users.xlsx";
        FesodSheet.write(fileName, MyUser.class).sheet("用户列表").doWrite(collect);
    }

}
