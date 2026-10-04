package com.waste.power.controller;

import java.math.BigDecimal;
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
import com.waste.power.dto.PitPayload;
import com.waste.power.service.StoragePitService;

/**
 * 垃圾储坑接口：坑位剖面数据、卸料入坑、抓斗投料、渗滤液抽出、发酵推进。
 */
@RestController
@RequestMapping("/api/pits")
public class PitController {

    private final StoragePitService pitService;

    public PitController(StoragePitService pitService) {
        this.pitService = pitService;
    }

    @GetMapping
    public ApiResponse<List<PitPayload.VO>> list() {
        return ApiResponse.success(pitService.list());
    }

    @GetMapping("/{pitNo}")
    public ApiResponse<PitPayload.VO> detail(@PathVariable String pitNo) {
        return ApiResponse.success(pitService.get(pitNo));
    }

    @PostMapping("/{pitNo}/unload")
    public ApiResponse<PitPayload.VO> unload(@PathVariable String pitNo,
            @RequestBody PitPayload.UnloadRequest request) {
        return ApiResponse.success("卸料入坑成功", pitService.unload(pitNo, request));
    }

    @PostMapping("/{pitNo}/feed")
    public ApiResponse<PitPayload.VO> feed(@PathVariable String pitNo, @RequestBody PitPayload.FeedRequest request) {
        return ApiResponse.success("抓斗投料成功，已计入目标炉当班入炉量", pitService.feed(pitNo, request));
    }

    @PostMapping("/{pitNo}/leachate")
    public ApiResponse<PitPayload.VO> pump(@PathVariable String pitNo,
            @RequestBody PitPayload.LeachateRequest request) {
        return ApiResponse.success("渗滤液抽出量已登记", pitService.pump(pitNo, request));
    }

    @PostMapping("/{pitNo}/ferment")
    public ApiResponse<PitPayload.VO> ferment(@PathVariable String pitNo,
            @RequestParam(required = false) BigDecimal days,
            @RequestParam(required = false) BigDecimal tempC) {
        return ApiResponse.success("堆酵状态已更新", pitService.ferment(pitNo, days, tempC));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(pitService.dict());
    }
}
