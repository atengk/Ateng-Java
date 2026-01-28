package io.github.atengk.listener;

import io.github.atengk.entity.MyUser;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public class ValidationAllUserListener extends AnalysisEventListener<MyUser> {

    /**
     * 校验通过的数据
     */
    private final List<MyUser> successList = new ArrayList<>();

    /**
     * 校验失败的错误信息
     */
    private final List<String> errorList = new ArrayList<>();

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();
        Integer excelRowNum = rowIndex + 1;
        errorList.add("第" + excelRowNum + "行数据解析失败：" + exception.getMessage());
    }

    @Override
    public void invoke(MyUser myUser, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();
        validate(myUser, rowIndex);
        successList.add(myUser);
    }

    /**
     * 用户导入数据校验逻辑
     *
     * @param data     当前行解析后的数据对象
     * @param rowIndex Excel 行号，从 0 开始
     */
    private void validate(MyUser data, Integer rowIndex) {

        Integer excelRowNum = rowIndex + 1;

        if (ObjectUtils.isEmpty(data.getName())) {
            throw new IllegalArgumentException("第" + excelRowNum + "行：姓名不能为空");
        }

        if (data.getName().length() > 50) {
            throw new IllegalArgumentException("第" + excelRowNum + "行：姓名长度不能超过 50");
        }

        if (data.getAge() == null) {
            throw new IllegalArgumentException("第" + excelRowNum + "行：年龄不能为空");
        }

        if (data.getAge() < 0 || data.getAge() > 150) {
            throw new IllegalArgumentException("第" + excelRowNum + "行：年龄必须在 0 到 150 之间");
        }

        if (ObjectUtils.isEmpty(data.getPhoneNumber())) {
            throw new IllegalArgumentException("第" + excelRowNum + "行：手机号不能为空");
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }

    public List<MyUser> getSuccessList() {
        return successList;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public boolean hasError() {
        return !errorList.isEmpty();
    }

}
