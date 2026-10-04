package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.IntakePayload;
import com.waste.power.dto.SourceSummaryDTO;
import com.waste.power.service.IntakeService;

/**
 * 进厂与计量接口：地磅流水、当日统计、来源区域汇总、车次登记与编辑。
 */
@RestController
@RequestMapping("/api/intake")
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @GetMapping("/records")
    public ApiResponse<List<IntakePayload.VO>> records() {
        return ApiResponse.success(intakeService.latest());
    }

    @GetMapping("/daily-stat")
    public ApiResponse<Map<String, Object>> dailyStat() {
        return ApiResponse.success(intakeService.dailyStat());
    }

    @GetMapping("/source-summary")
    public ApiResponse<List<SourceSummaryDTO>> sourceSummary() {
        return ApiResponse.success(intakeService.sourceSummary());
    }

    @PostMapping("/records")
    public ApiResponse<IntakePayload.VO> create(@RequestBody IntakePayload.Request request) {
        return ApiResponse.success("过磅登记成功", intakeService.create(request));
    }

    @PutMapping("/records/{id}")
    public ApiResponse<IntakePayload.VO> update(@PathVariable Long id, @RequestBody IntakePayload.Request request) {
        return ApiResponse.success("过磅记录已更新，净重已重算", intakeService.update(id, request));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        intakeService.delete(id);
        return ApiResponse.success("过磅记录已删除", null);
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(intakeService.dict());
    }
}
