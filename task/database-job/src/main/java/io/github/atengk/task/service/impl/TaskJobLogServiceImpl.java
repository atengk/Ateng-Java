package io.github.atengk.task.service.impl;

import io.github.atengk.task.entity.TaskJobLog;
import io.github.atengk.task.mapper.TaskJobLogMapper;
import io.github.atengk.task.service.ITaskJobLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 任务执行日志表 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2026-02-11
 */
@Service
public class TaskJobLogServiceImpl extends ServiceImpl<TaskJobLogMapper, TaskJobLog> implements ITaskJobLogService {

}
