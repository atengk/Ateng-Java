package local.ateng.java.snailjob.task;

import cn.hutool.core.convert.Convert;
import com.aizuda.snailjob.client.core.annotation.ExecutorMethodRegister;
import com.aizuda.snailjob.client.core.strategy.ExecutorMethod;
import local.ateng.java.snailjob.vo.MyUserVo;
import lombok.extern.slf4j.Slf4j;

@ExecutorMethodRegister(scene = RetryExecutorTask.SCENE)
@Slf4j
public class RetryExecutorTask implements ExecutorMethod {
    /**
     * 自定义场景值
     */
    public final static String SCENE = "myRetryExecutorTask";

    @Override
    public Object doExecute(Object params) {
        // 将特定类型的 Object 对象指定为 Object[]
        Object[] args = (Object[]) params;
        MyUserVo myUserVo = Convert.convert(MyUserVo.class, args[0]);
        log.info("进入手动重试方法,参数信息是{}", myUserVo);
        return true;
    }
}
