package local.ateng.java.snailjob.controller;

import com.aizuda.snailjob.client.job.core.dto.JobResponseVO;
import com.aizuda.snailjob.client.job.core.enums.AllocationAlgorithmEnum;
import com.aizuda.snailjob.client.job.core.enums.TriggerTypeEnum;
import com.aizuda.snailjob.client.job.core.openapi.SnailJobOpenApi;
import com.aizuda.snailjob.common.core.enums.BlockStrategyEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/openapi")
public class OpenApiController {


    /**
     * 新增集群模式的任务
     *
     * @param jobName 任务名称
     * @return 任务id
     */
    @GetMapping("/addClusterJob")
    public Long addClusterJob(String jobName) {
        return SnailJobOpenApi.addClusterJob()
                .setRouteKey(AllocationAlgorithmEnum.RANDOM)
                .setJobName(jobName)
                .setExecutorInfo("testJobExecutor")
                .setExecutorTimeout(30)
                .setDescription("add")
                .setBlockStrategy(BlockStrategyEnum.DISCARD)
                .setMaxRetryTimes(1)
                .setTriggerType(TriggerTypeEnum.SCHEDULED_TIME)
                .setTriggerInterval(String.valueOf(60))
                .addArgsStr("测试数据", 123)
                .addArgsStr("addArg", "args")
                .setRetryInterval(3)
                .execute();
    }

    /**
     * 查看任务详情
     *
     * @param jobId
     * @return 任务详情
     */
    @GetMapping("/queryJob")
    public JobResponseVO queryJob(Long jobId){
        return SnailJobOpenApi.getJobDetail(jobId).execute();
    }

}
