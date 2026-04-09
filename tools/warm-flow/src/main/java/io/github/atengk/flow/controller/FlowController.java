package io.github.atengk.flow.controller;

import io.github.atengk.flow.service.WarmFlowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * WarmFlow 流程控制器
 *
 * 提供完整流程操作：部署、发布、启动、审批、查询任务
 *
 * @author Ateng
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/flow")
public class FlowController {

    @Resource
    private WarmFlowService flowService;

    /**
     * 1. 部署流程
     */
    @PostMapping("/deploy")
    public Long deploy() {
        return flowService.deploy("flow/demo-flow.json");
    }

    /**
     * 2. 发布流程
     */
    @PostMapping("/publish")
    public String publish(@RequestParam String flowCode) {
        flowService.publish(flowCode);
        return "publish success";
    }

    /**
     * 3. 启动流程
     */
    @PostMapping("/start")
    public Long start(@RequestParam String flowCode,
                      @RequestParam String businessId,
                      @RequestParam String userId) {
        return flowService.start(flowCode, businessId, userId);
    }

    /**
     * 4. 查询当前任务ID（调试用）
     */
    @GetMapping("/tasks")
    public List<Long> tasks(@RequestParam Long insId) {
        return flowService.getCurrentTaskIds(insId);
    }

    /**
     * 5. 审批通过
     */
    @PostMapping("/approve")
    public String approve(@RequestParam Long insId,
                          @RequestParam String flowCode,
                          @RequestParam String userId) {
        flowService.approve(insId, flowCode, userId);
        return "approve success";
    }

    /**
     * 6. 驳回
     */
    @PostMapping("/reject")
    public String reject(@RequestParam Long insId,
                         @RequestParam String flowCode,
                         @RequestParam String userId) {
        flowService.reject(insId, flowCode, userId);
        return "reject success";
    }

}