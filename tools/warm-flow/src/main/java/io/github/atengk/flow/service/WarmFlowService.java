package io.github.atengk.flow.service;

import cn.hutool.core.io.resource.ResourceUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * WarmFlow 流程服务
 *
 * @author Ateng
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
public class WarmFlowService {

    private final DefService defService;
    private final InsService insService;
    private final TaskService taskService;

    /**
     * 部署流程（导入 JSON）
     */
    public Long deploy(String path) {
        InputStream is = ResourceUtil.getStream(path);
        Definition definition = defService.importIs(is);
        return definition.getId();
    }

    /**
     * 根据 flowCode 获取定义ID
     */
    public Long getDefId(String flowCode) {
        return defService.queryByCodeList(Collections.singletonList(flowCode))
                .stream()
                .findFirst()
                .map(Definition::getId)
                .orElseThrow(() -> new RuntimeException("流程不存在"));
    }

    /**
     * 发布流程
     */
    public void publish(String flowCode) {
        defService.publish(getDefId(flowCode));
    }

    /**
     * 启动流程实例
     */
    public Long start(String flowCode, String businessId, String userId) {
        FlowParams params = buildBaseParams(flowCode, userId);

        Instance instance = insService.start(businessId, params);
        return instance.getId();
    }

    /**
     * 审批通过（基于实例ID）
     */
    public void approve(Long insId, String flowCode, String userId) {
        FlowParams params = buildBaseParams(flowCode, userId)
                .skipType(SkipType.PASS.getKey());

        taskService.skipByInsId(insId, params);
    }

    /**
     * 审批拒绝
     */
    public void reject(Long insId, String flowCode, String userId) {
        FlowParams params = buildBaseParams(flowCode, userId)
                .skipType(SkipType.REJECT.getKey());

        taskService.skipByInsId(insId, params);
    }

    /**
     * 查询当前任务
     */
    public List<Long> getCurrentTaskIds(Long insId) {
        return taskService.list(
                        FlowEngine.newTask().setInstanceId(insId)
                ).stream()
                .map(task -> task.getId())
                .toList();
    }

    /**
     * 构建基础 FlowParams（核心）
     */
    private FlowParams buildBaseParams(String flowCode, String userId) {
        return FlowParams.build()
                .flowCode(flowCode)
                .handler(userId)
                .permissionFlag(Arrays.asList(
                        "role:admin",
                        "user:" + userId
                ));
    }

}