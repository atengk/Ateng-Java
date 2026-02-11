package io.github.atengk.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.atengk.task.entity.TaskJob;
import io.github.atengk.task.mapper.TaskJobMapper;
import io.github.atengk.task.service.ITaskJobService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 任务定义表 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2026-02-11
 */
@Service
public class TaskJobServiceImpl extends ServiceImpl<TaskJobMapper, TaskJob> implements ITaskJobService {

}
