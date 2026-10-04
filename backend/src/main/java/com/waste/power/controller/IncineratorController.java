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
import com.waste.power.dto.FurnaceMonthlyDTO;
import com.waste.power.dto.FurnacePayload;
import com.waste.power.dto.FurnaceTypeStatDTO;
import com.waste.power.service.IncineratorService;

/**
 * 焚烧炉与炉型接口：
 * /api/incinerators 返回的数据带 furnaceType 与各子类专属字段（由 @DiscriminatorFormula 判别子类）；
 * /api/incinerators/stats/type 为按子类 TYPE() 统计；
 * /api/incinerators/fast-grate 为 treat() 访问子类专属字段的查询。
 */
@RestController
@RequestMapping("/api/incinerators")
public class IncineratorController {

    private final IncineratorService incineratorService;

    public IncineratorController(IncineratorService incineratorService) {
        this.incineratorService = incineratorService;
    }

    @GetMapping
    public ApiResponse<List<FurnacePayload.VO>> list() {
        return ApiResponse.success(incineratorService.list());
    }

    @GetMapping("/{furnaceCode}")
    public ApiResponse<FurnacePayload.VO> detail(@PathVariable String furnaceCode) {
        return ApiResponse.success(incineratorService.get(furnaceCode));
    }

    @PostMapping("/{furnaceCode}/run")
    public ApiResponse<FurnacePayload.VO> run(@PathVariable String furnaceCode,
            @RequestBody FurnacePayload.RunRequest request) {
        return ApiResponse.success("燃烧状态已推进", incineratorService.run(furnaceCode, request));
    }

    @PostMapping("/{furnaceCode}/judge")
    public ApiResponse<Map<String, Object>> judge(@PathVariable String furnaceCode) {
        return ApiResponse.success(incineratorService.judge(furnaceCode));
    }

    @GetMapping("/stats/type")
    public ApiResponse<List<FurnaceTypeStatDTO>> statsByType() {
        return ApiResponse.success(incineratorService.statsByType());
    }

    @GetMapping("/stats/monthly")
    public ApiResponse<List<FurnaceMonthlyDTO>> monthlyStats() {
        return ApiResponse.success(incineratorService.monthlyStats());
    }

    @GetMapping("/fast-grate")
    public ApiResponse<List<FurnacePayload.VO>> fastGrate(
            @RequestParam(defaultValue = "6.0") BigDecimal speed) {
        return ApiResponse.success(incineratorService.fastGrateFurnaces(speed));
    }

    @GetMapping("/logs")
    public ApiResponse<List<FurnacePayload.LogVO>> logs(@RequestParam(required = false) String furnaceCode) {
        return ApiResponse.success(incineratorService.logs(furnaceCode));
    }

    @GetMapping("/dict")
    public ApiResponse<Map<String, Object>> dict() {
        return ApiResponse.success(incineratorService.dict());
    }
}
