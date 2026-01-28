package io.github.atengk.listener;

import io.github.atengk.entity.MyUser;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MyUserBatchReadListener implements ReadListener<MyUser> {

    private static final Logger log = LoggerFactory.getLogger(MyUserBatchReadListener.class);

    /**
     * 单批次最大数据量
     */
    private static final int BATCH_SIZE = 400;

    /**
     * 当前批次缓存数据
     */
    private final List<MyUser> batchList = new ArrayList<>(BATCH_SIZE);

    /**
     * 成功处理的数据总量，仅用于测试统计
     */
    private int totalCount = 0;

    @Override
    public void invoke(MyUser myUser, AnalysisContext analysisContext) {
        batchList.add(myUser);

        if (batchList.size() >= BATCH_SIZE) {
            saveBatch();
            batchList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        if (!batchList.isEmpty()) {
            saveBatch();
            batchList.clear();
        }

        log.info("Excel 导入完成，总处理数据量：{}", totalCount);
    }

    /**
     * 模拟批量入库
     */
    private void saveBatch() {
        int size = batchList.size();
        totalCount += size;

        log.info("模拟入库，当前批次大小：{}，累计处理：{}", size, totalCount);

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
