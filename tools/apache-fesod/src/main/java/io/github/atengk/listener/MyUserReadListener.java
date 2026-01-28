package io.github.atengk.listener;

import io.github.atengk.entity.MyUser;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;

import java.util.ArrayList;
import java.util.List;

public class MyUserReadListener implements ReadListener<MyUser> {

    private final List<MyUser> dataList = new ArrayList<>();

    @Override
    public void invoke(MyUser myUser, AnalysisContext analysisContext) {
        dataList.add(myUser);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }

    public List<MyUser> getDataList() {
        return dataList;
    }

}
