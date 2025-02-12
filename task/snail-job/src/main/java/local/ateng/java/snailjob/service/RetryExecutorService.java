package local.ateng.java.snailjob.service;

import com.aizuda.snailjob.client.core.retryer.RetryTaskTemplateBuilder;
import com.aizuda.snailjob.client.core.retryer.SnailJobTemplate;
import local.ateng.java.snailjob.task.RetryExecutorTask;
import local.ateng.java.snailjob.vo.MyUserVo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RetryExecutorService {

    public void myExecutorMethod(){
        MyUserVo userVo = new MyUserVo(10010L, "阿腾", 25, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        SnailJobTemplate snailJobTemplate = RetryTaskTemplateBuilder.newBuilder()
                // 手动指定场景名称
                .withScene(RetryExecutorTask.SCENE)
                // 指定要执行的任务
                .withExecutorMethod(RetryExecutorTask.class)
                // 指定参数
                .withParam(userVo)
                .build();
        // 执行模板
        snailJobTemplate.executeRetry();
    }

}
