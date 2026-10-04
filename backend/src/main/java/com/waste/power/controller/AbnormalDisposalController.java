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
import com.waste.power.dto.DisposalPayload;
import com.waste.power.service.AbnormalDisposalService;

/**
 * 异常工况处置单接口：
 * 自动触发（炉况判定失败 / 同炉严重告警）由 IncineratorService、EnvironmentService 在服务内完成，
 * 这里提供人工发起、措施记录/申请复核、他人复核恢复、关闭、告警数据缺失选择、
 * 列表/详情/证据/操作轨迹/实时恢复条件查询，以及冻结炉号清单。
 */
@RestController
@RequestMapping("/api/disposals")
public class AbnormalDisposalController {

    private final AbnormalDisposalService disposalService;

    public AbnormalDisposalController(AbnormalDisposalService disposalService) {
        this.disposalService = disposalService;
    }

    /** 列表：默认仅未关闭有效单；all=true 返回最近 50 张（含已关闭） */
    @GetMapping
    public ApiResponse<List<DisposalPayload.VO>> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(disposalService.list(activeOnly));
    }

    @GetMapping("/{id}")
    public ApiResponse<DisposalPayload.VO> detail(@PathVariable Long id) {
        return ApiResponse.success(disposalService.detail(id));
    }

    @GetMapping("/{id}/evidences")
    public ApiResponse<List<DisposalPayload.AlarmEvidenceVO>> evidences(@PathVariable Long id) {
        return ApiResponse.success(disposalService.evidences(id));
    }

    @GetMapping("/{id}/traces")
    public ApiResponse<List<DisposalPayload.TraceVO>> traces(@PathVariable Long id) {
        return ApiResponse.success(disposalService.traces(id));
    }

    /** 实时恢复条件（燃烧达标 / 严重告警解除 / 数据缺失选择影响 / 投料权限） */
    @GetMapping("/{id}/condition")
    public ApiResponse<DisposalPayload.ConditionVO> condition(@PathVariable Long id) {
        return ApiResponse.success(disposalService.condition(id));
    }

    /** 当前投料被冻结的炉号（炉况入口、储坑投料页据此置灰并保持多端一致） */
    @GetMapping("/frozen-furnaces")
    public ApiResponse<List<String>> frozenFurnaces() {
        return ApiResponse.success(disposalService.frozenFurnaceCodes());
    }

    /** 人工发起（同炉已有有效单时合并证据，返回既有单，不会重复冻结） */
    @PostMapping("/open")
    public ApiResponse<DisposalPayload.VO> open(@RequestBody DisposalPayload.OpenRequest request) {
        return ApiResponse.success("已开立/合并异常工况处置单并冻结该炉投料", disposalService.manualOpen(request));
    }

    /** 负责人记录处置措施（可同时申请复核） */
    @PostMapping("/{id}/measures")
    public ApiResponse<DisposalPayload.VO> measure(@PathVariable Long id,
            @RequestBody DisposalPayload.MeasureRequest request) {
        boolean requestReview = Boolean.TRUE.equals(request.getRequestReview());
        return ApiResponse.success(requestReview ? "措施已记录并申请复核" : "处置措施已记录",
                disposalService.recordMeasure(id, request));
    }

    /** 另一名复核人员执行复核恢复，或退回继续处置 */
    @PostMapping("/{id}/review")
    public ApiResponse<DisposalPayload.VO> review(@PathVariable Long id,
            @RequestBody DisposalPayload.ReviewRequest request) {
        return ApiResponse.success("REJECT".equals(request.getChoice())
                ? "复核不通过，已退回处置中（投料保持冻结）"
                : "复核通过，燃烧已恢复且严重告警解除，投料联锁已解除",
                disposalService.reviewRecover(id, request));
    }

    /** 关闭已恢复处置单 */
    @PostMapping("/{id}/close")
    public ApiResponse<DisposalPayload.VO> close(@PathVariable Long id,
            @RequestBody(required = false) DisposalPayload.CloseRequest request) {
        return ApiResponse.success("处置单已关闭，闭环结束", disposalService.close(id,
                request == null ? new DisposalPayload.CloseRequest() : request));
    }

    /** 告警数据暂不可用时明确选择：继续冻结等待补齐 / 记录理由后关闭但保持投料冻结 */
    @PostMapping("/{id}/missing-choice")
    public ApiResponse<DisposalPayload.VO> missingChoice(@PathVariable Long id,
            @RequestBody DisposalPayload.MissingChoiceRequest request) {
        return ApiResponse.success("已记录告警数据缺失处置选择", disposalService.chooseWhenAlarmMissing(id, request));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(disposalService.dict());
    }
}
