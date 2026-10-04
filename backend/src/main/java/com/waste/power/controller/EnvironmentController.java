package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.AshPayload;
import com.waste.power.dto.DashboardVO;
import com.waste.power.dto.EmissionExceedRankDTO;
import com.waste.power.dto.EmissionPayload;
import com.waste.power.service.EnvironmentService;

/**
 * 环保排放与灰渣接口：排放登记（超标自动告警）、告警处理、超标排行、灰渣与渗滤液台账、仪表盘。
 */
@RestController
@RequestMapping("/api/environment")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardVO> dashboard() {
        return ApiResponse.success(environmentService.dashboard());
    }

    @GetMapping("/emissions")
    public ApiResponse<List<EmissionPayload.VO>> emissions() {
        return ApiResponse.success(environmentService.emissions());
    }

    @GetMapping("/gauges")
    public ApiResponse<List<EmissionPayload.VO>> gauges() {
        return ApiResponse.success(environmentService.gauges());
    }

    @PostMapping("/emissions")
    public ApiResponse<EmissionPayload.VO> register(@RequestBody EmissionPayload.Request request) {
        EmissionPayload.VO vo = environmentService.register(request);
        return ApiResponse.success(Boolean.TRUE.equals(vo.getExceeded())
                ? "排放已登记：实测值超标，已自动登记环保告警"
                : "排放已登记：指标达标", vo);
    }

    @GetMapping("/alarms")
    public ApiResponse<List<EmissionPayload.AlarmVO>> alarms() {
        return ApiResponse.success(environmentService.alarms());
    }

    @PostMapping("/alarms/{id}/handle")
    public ApiResponse<EmissionPayload.AlarmVO> handle(@PathVariable Long id,
            @RequestBody EmissionPayload.HandleRequest request) {
        return ApiResponse.success("告警已处理", environmentService.handleAlarm(id, request));
    }

    @GetMapping("/exceed-rank")
    public ApiResponse<List<EmissionExceedRankDTO>> exceedRank() {
        return ApiResponse.success(environmentService.exceedRank());
    }

    @GetMapping("/ash")
    public ApiResponse<List<AshPayload.VO>> ashRecords() {
        return ApiResponse.success(environmentService.ashRecords());
    }

    @PostMapping("/ash")
    public ApiResponse<AshPayload.VO> registerAsh(@RequestBody AshPayload.Request request) {
        return ApiResponse.success("灰渣与渗滤液台账已登记", environmentService.registerAsh(request));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(environmentService.dict());
    }
}
