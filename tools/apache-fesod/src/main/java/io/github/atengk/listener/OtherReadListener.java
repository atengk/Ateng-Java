package io.github.atengk.listener;

import io.github.atengk.entity.Other;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;

import java.util.ArrayList;
import java.util.List;

public class OtherReadListener implements ReadListener<Other> {

    private final List<Other> dataList = new ArrayList<>();

    @Override
    public void invoke(Other other, AnalysisContext analysisContext) {
        dataList.add(other);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }

    public List<Other> getDataList() {
        return dataList;
    }

}
