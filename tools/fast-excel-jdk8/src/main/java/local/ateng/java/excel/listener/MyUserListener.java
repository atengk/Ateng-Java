package local.ateng.java.excel.listener;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.exception.ExcelDataConvertException;
import local.ateng.java.excel.entity.MyUser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyUserListener extends AnalysisEventListener<MyUser> {
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("解析失败: {}", exception.getMessage());
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException ex = (ExcelDataConvertException) exception;
            String str = StrUtil.format("第 {} 行, 第 {} 列解析异常", ex.getRowIndex(), ex.getColumnIndex());
            log.error(str);
            throw new RuntimeException(str);
        }
    }

    @Override
    public void invoke(MyUser data, AnalysisContext context) {
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
    }

}
