package local.ateng.java.snailjob.task;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Snail Job 执行器
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-11
 */
@Component
@Slf4j
public class DemoTask {

    @JobExecutor(name = "demoJob")
    public ExecuteResult jobExecute(JobArgs jobArgs) {
        String str = StrUtil.format("执行[demoJob]任务, JobArgs={}, 线程={}", jobArgs, Thread.currentThread());
        log.info(str);
        return ExecuteResult.success(str);
    }

}
