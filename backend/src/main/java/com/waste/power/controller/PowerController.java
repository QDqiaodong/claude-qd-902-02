package com.waste.power.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.waste.power.common.ApiResponse;
import com.waste.power.dto.PowerPayload;
import com.waste.power.service.PowerService;

/**
 * 余热锅炉与汽轮发电接口：蒸汽工况、发电日结（相邻表码增量口径）、表计回拨/更换处置与校表。
 */
@RestController
@RequestMapping("/api/power")
public class PowerController {

    private final PowerService powerService;

    public PowerController(PowerService powerService) {
        this.powerService = powerService;
    }

    @GetMapping("/boilers")
    public ApiResponse<List<PowerPayload.BoilerVO>> boilers() {
        return ApiResponse.success(powerService.boilers());
    }

    @GetMapping("/turbines")
    public ApiResponse<List<PowerPayload.TurbineVO>> turbines() {
        return ApiResponse.success(powerService.turbines());
    }

    /** 发电日结列表（可按机组过滤）：当前表码、当日增量、基数来源、异常状态与每日计算依据 */
    @GetMapping("/daily-settlements")
    public ApiResponse<List<PowerPayload.DailySettlementVO>> dailySettlements(
            @RequestParam(name = "unitNo", required = false) String unitNo) {
        return ApiResponse.success(powerService.settlements(unitNo));
    }

    /** 日结/表计处置操作轨迹（回拨、换表、封账、校表、重复请求拒绝全部留痕） */
    @GetMapping("/daily-settlements/{id}/traces")
    public ApiResponse<List<PowerPayload.MeterTraceVO>> traces(@PathVariable Long id) {
        return ApiResponse.success(powerService.traces(id));
    }

    /** 某机组最近的日结/表计处置轨迹（含被拒日结尚无单 id 的失败留痕） */
    @GetMapping("/turbines/{unitNo}/meter-traces")
    public ApiResponse<List<PowerPayload.MeterTraceVO>> unitTraces(@PathVariable String unitNo) {
        return ApiResponse.success(powerService.unitTraces(unitNo));
    }

    @PostMapping("/boilers/{boilerNo}/adjust")
    public ApiResponse<PowerPayload.BoilerVO> adjustBoiler(@PathVariable String boilerNo,
            @RequestBody PowerPayload.BoilerRequest request) {
        return ApiResponse.success("锅炉蒸汽工况已更新", powerService.adjustBoiler(boilerNo, request));
    }

    /**
     * 发电日结登记：提交当日主电能表抄见表码，按相邻表码计算当日发电增量，
     * 再算上网电量与厂用电率。同机组同日重复请求幂等返回，不产生第二份日结。
     */
    @PostMapping("/turbines/{unitNo}/daily-settlement")
    public ApiResponse<PowerPayload.DailySettlementVO> settle(@PathVariable String unitNo,
            @RequestBody PowerPayload.DailySettleRequest request) {
        PowerPayload.DailySettlementVO vo = powerService.settle(unitNo, request);
        String message = "SUSPENDED".equals(vo.getStatus())
                ? "已暂停日结等待校表"
                : ("SEAL_NEW_BASE".equals(vo.getDecision()) ? "旧表已封账并按新基数完成日结" : "发电日结已完成");
        return ApiResponse.success(message, vo);
    }

    /** 暂停日结后的现场校表登记：确认有效基数，SUSPENDED → VERIFIED */
    @PostMapping("/daily-settlements/{id}/verify")
    public ApiResponse<PowerPayload.DailySettlementVO> verify(@PathVariable Long id,
            @RequestBody PowerPayload.MeterVerifyRequest request) {
        return ApiResponse.success("校表基数已确认，请对该统计日重新日结", powerService.verify(id, request));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(powerService.dict());
    }
}
