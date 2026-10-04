package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.IntakeRecord;
import com.waste.power.dto.CategoryShareDTO;
import com.waste.power.dto.IntakePayload;
import com.waste.power.dto.SourceSummaryDTO;
import com.waste.power.repository.IntakeRecordRepository;

/**
 * 进厂与计量业务：净重计算与校验、当日车次统计、来源区域汇总、类别占比。
 */
@Service
@Transactional
public class IntakeService {

    private final IntakeRecordRepository intakeRepository;

    public IntakeService(IntakeRecordRepository intakeRepository) {
        this.intakeRepository = intakeRepository;
    }

    /**
     * 地磅流水（最近 50 车次）
     */
    @Transactional(readOnly = true)
    public List<IntakePayload.VO> latest() {
        return intakeRepository.findTop50ByOrderByIntakeTimeDesc().stream()
                .map(IntakePayload.VO::from)
                .toList();
    }

    /**
     * 按来源行政区划汇总进厂量与热值（JPQL）
     */
    @Transactional(readOnly = true)
    public List<SourceSummaryDTO> sourceSummary() {
        return intakeRepository.summaryByRegion();
    }

    /**
     * 当日进厂统计：车次、净重合计、平均净重，以及按垃圾类别的占比。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> dailyStat() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        var stat = intakeRepository.statBetween(start, end);
        long trips = stat.getTrips() == null ? 0L : stat.getTrips();
        BigDecimal netTon = stat.getNetWeightTon() == null ? BigDecimal.ZERO : stat.getNetWeightTon();
        double avg = stat.getAvgNetWeightTon() == null ? 0D : stat.getAvgNetWeightTon();
        result.put("date", today.toString());
        result.put("trips", trips);
        result.put("netWeightTon", netTon.setScale(2, RoundingMode.HALF_UP));
        result.put("avgNetWeightTon", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));

        List<CategoryShareDTO> shares = intakeRepository.categoryShares(start, end);
        List<Map<String, Object>> categories = new ArrayList<>();
        for (String code : Dict.WASTE_CATEGORIES) {
            BigDecimal ton = BigDecimal.ZERO;
            long count = 0L;
            for (CategoryShareDTO share : shares) {
                if (code.equals(share.getWasteCategory())) {
                    ton = share.getNetWeightTon() == null ? BigDecimal.ZERO : share.getNetWeightTon();
                    count = share.getTrips() == null ? 0L : share.getTrips();
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", code);
            item.put("name", Dict.categoryName(code));
            item.put("netWeightTon", ton.setScale(2, RoundingMode.HALF_UP));
            item.put("trips", count);
            item.put("rate", netTon.compareTo(BigDecimal.ZERO) == 0 ? 0D : ton.doubleValue() / netTon.doubleValue());
            categories.add(item);
        }
        result.put("categories", categories);
        return result;
    }

    /**
     * 新增进厂车次：净重 = 毛重 − 皮重，必须为正。
     */
    public IntakePayload.VO create(IntakePayload.Request request) {
        IntakeRecord record = new IntakeRecord();
        applyRequest(record, request);
        return IntakePayload.VO.from(intakeRepository.save(record));
    }

    /**
     * 编辑进厂车次（净重自动重算并校验）
     */
    public IntakePayload.VO update(Long id, IntakePayload.Request request) {
        IntakeRecord record = intakeRepository.findById(id)
                .orElseThrow(() -> new BizException("进厂车次不存在：id=" + id));
        applyRequest(record, request);
        return IntakePayload.VO.from(intakeRepository.save(record));
    }

    public void delete(Long id) {
        if (!intakeRepository.existsById(id)) {
            throw new BizException("进厂车次不存在：id=" + id);
        }
        intakeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();
        for (String code : Dict.WASTE_CATEGORIES) {
            categories.add(Map.of("code", code, "name", Dict.categoryName(code),
                    "calorific", Dict.categoryCalorific(code)));
        }
        List<Map<String, Object>> regions = new ArrayList<>();
        for (String region : Dict.SOURCE_REGIONS) {
            regions.add(Map.of("name", region, "stations", Dict.stations(region)));
        }
        dict.put("categories", categories);
        dict.put("regions", regions);
        dict.put("scaleNos", Dict.SCALE_NOS);
        dict.put("maxNetTon", Dict.MAX_TRUCK_NET_TON);
        return dict;
    }

    // ---------------- 内部校验 ----------------

    private void applyRequest(IntakeRecord record, IntakePayload.Request request) {
        if (isBlank(request.getTruckNo())) {
            throw new BizException("车牌号不能为空");
        }
        if (!Dict.isValidCategory(request.getWasteCategory())) {
            throw new BizException("垃圾类别不合法：" + request.getWasteCategory());
        }
        if (!Dict.isValidRegion(request.getSourceRegion())) {
            throw new BizException("来源行政区划不合法：" + request.getSourceRegion());
        }
        if (!Dict.isValidStation(request.getSourceRegion(), request.getTransferStation())) {
            throw new BizException("中转站不属于该行政区划：" + request.getSourceRegion() + " / " + request.getTransferStation());
        }
        BigDecimal gross = request.getGrossWeightTon();
        BigDecimal tare = request.getTareWeightTon();
        if (gross == null || tare == null) {
            throw new BizException("毛重与皮重均不能为空");
        }
        if (gross.compareTo(BigDecimal.ZERO) <= 0 || tare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("毛重与皮重必须大于 0");
        }
        // 规则：净重 = 毛重 − 皮重，且必须为正
        BigDecimal net = gross.subtract(tare);
        if (net.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("净重必须为正：毛重 " + gross + " t − 皮重 " + tare + " t = " + net + " t，请核对地磅数据");
        }
        if (net.compareTo(Dict.MAX_TRUCK_NET_TON) > 0) {
            throw new BizException("单车净重 " + net + " t 超出地磅量程上限 " + Dict.MAX_TRUCK_NET_TON + " t");
        }
        if (Dict.isRestricted(request.getWasteCategory()) && isBlank(request.getRemark())) {
            throw new BizException("有害垃圾须在备注中注明预处理措施（分拣/暂存/危废转移联单）");
        }

        record.setTruckNo(request.getTruckNo().trim());
        record.setSourceRegion(request.getSourceRegion());
        record.setTransferStation(request.getTransferStation());
        record.setWasteCategory(request.getWasteCategory());
        record.setGrossWeightTon(gross);
        record.setTareWeightTon(tare);
        record.setNetWeightTon(net);
        record.setScaleNo(isBlank(request.getScaleNo()) ? Dict.SCALE_NOS.get(0) : request.getScaleNo());
        record.setCalorificValue(request.getCalorificValue() == null
                ? Dict.categoryCalorific(request.getWasteCategory())
                : request.getCalorificValue());
        record.setDriverName(request.getDriverName());
        record.setUnloadPit(request.getUnloadPit());
        record.setRemark(request.getRemark());
        // 默认值放到 service：未填进厂时间则取当前时间
        record.setIntakeTime(request.getIntakeTime() == null ? LocalDateTime.now() : request.getIntakeTime());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
