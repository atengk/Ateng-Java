package local.ateng.java.excel.listener;

import cn.hutool.core.util.NumberUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import local.ateng.java.excel.entity.MyUser;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class MyUserMapListener extends AnalysisEventListener<Map<Integer, String>> {
    public final static List<MyUser> userList = new CopyOnWriteArrayList<>();

    @Override
    public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
        // 获取数据
        // {0=1525743431821710000, 1=任文昊, 2=49, 3=17734837677, 4=聪健.叶@yahoo.com, 5=33.23, 6=0.06%, 7=2025年02月08日, 8=河北省, 9=惠州, 10=2025-02-08 15:11:50}
        //System.out.println(dataMap);
        String idValue = dataMap.get(0);
        String nameValue = dataMap.get(1);
        String ageValue = dataMap.get(2);
        String phoneValue = dataMap.get(3);
        String emailValue = dataMap.get(4);
        String scoreValue = dataMap.get(5);
        String ratioValue = dataMap.get(6);
        String birthdayValue = dataMap.get(7);
        String provinceValue = dataMap.get(8);
        String cityValue = dataMap.get(9);
        String createTimeValue = dataMap.get(10);
        // 校验数据
        try {
            Long id = Long.valueOf(idValue);
            Integer age = Integer.valueOf(ageValue);
            BigDecimal score = new BigDecimal(scoreValue).setScale(2, BigDecimal.ROUND_HALF_UP);
            Double ratio = Double.valueOf(NumberUtil.parseDouble(ratioValue) / 100);
            LocalDate birthday = LocalDate.parse(birthdayValue, DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
            LocalDateTime createTime = LocalDateTime.parse(createTimeValue, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            MyUser myUser = MyUser.builder()
                    .id(id)
                    .name(nameValue)
                    .age(age)
                    .phoneNumber(phoneValue)
                    .email(emailValue)
                    .score(score)
                    .ratio(ratio)
                    .birthday(birthday)
                    .province(provinceValue)
                    .city(cityValue)
                    .createTime(createTime)
                    .build();
            userList.add(myUser);
        } catch (NumberFormatException e) {
            // 获取当前行号（从0开始）
            int rowNum = context.readRowHolder().getRowIndex();
            // 获取当前行的列数
            log.error("第{}行错误数据: {}", rowNum, dataMap);
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("数据加载完毕：{}", userList.size());
    }

}
