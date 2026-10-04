package com.waste.power.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waste.power.common.BizException;
import com.waste.power.common.Dict;
import com.waste.power.domain.Incinerator;
import com.waste.power.domain.StoragePit;
import com.waste.power.dto.PitPayload;
import com.waste.power.repository.IncineratorRepository;
import com.waste.power.repository.StoragePitRepository;

/**
 * 垃圾储坑业务：卸料入坑（库存不得超过容量）、抓斗投料到炉、渗滤液抽出登记。
 */
@Service
@Transactional
public class StoragePitService {

    private final StoragePitRepository pitRepository;
    private final IncineratorRepository incineratorRepository;
    private final AbnormalDisposalService disposalService;

    public StoragePitService(StoragePitRepository pitRepository, IncineratorRepository incineratorRepository,
            AbnormalDisposalService disposalService) {
        this.pitRepository = pitRepository;
        this.incineratorRepository = incineratorRepository;
        this.disposalService = disposalService;
    }

    @Transactional(readOnly = true)
    public List<PitPayload.VO> list() {
        return pitRepository.findAll().stream().map(PitPayload.VO::from).toList();
    }

    @Transactional(readOnly = true)
    public PitPayload.VO get(String pitNo) {
        return PitPayload.VO.from(requirePit(pitNo));
    }

    /**
     * 卸料入坑：库存超过容量不允许继续卸料（报错并给出可容纳余量）
     */
    public PitPayload.VO unload(String pitNo, PitPayload.UnloadRequest request) {
        StoragePit pit = requirePit(pitNo);
        BigDecimal unloadTon = request.getUnloadTon();
        if (unloadTon == null || unloadTon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("卸料量必须大于 0");
        }
        BigDecimal capacity = nvl(pit.getCapacityTon());
        BigDecimal stock = nvl(pit.getCurrentStockTon());
        BigDecimal free = capacity.subtract(stock);
        if (free.compareTo(unloadTon) < 0) {
            throw new BizException("储坑 " + pit.getPitNo() + " 剩余可容纳 " + free.setScale(2, RoundingMode.HALF_UP)
                    + " t，本次卸料 " + unloadTon.setScale(2, RoundingMode.HALF_UP) + " t 超出，禁止卸料");
        }
        pit.setCurrentStockTon(stock.add(unloadTon));
        if (request.getTruckNo() != null && !request.getTruckNo().isBlank()) {
            pit.setRemark("最近卸料车辆 " + request.getTruckNo());
        }
        refreshStatus(pit);
        return PitPayload.VO.from(pitRepository.save(pit));
    }

    /**
     * 抓斗投料：从储坑抓料投入目标焚烧炉
     */
    public PitPayload.VO feed(String pitNo, PitPayload.FeedRequest request) {
        StoragePit pit = requirePit(pitNo);
        BigDecimal feedTon = request.getFeedTon();
        if (feedTon == null || feedTon.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("投料量必须大于 0");
        }
        if (request.getGrabNo() == null || !Dict.GRAB_NOS.contains(request.getGrabNo())) {
            throw new BizException("抓斗编号不合法：" + request.getGrabNo());
        }
        Incinerator furnace = incineratorRepository.findByFurnaceCode(request.getTargetFurnaceCode())
                .orElseThrow(() -> new BizException("目标炉号不存在：" + request.getTargetFurnaceCode()));
        if ("SHUTDOWN".equals(furnace.getCombustionStatus())) {
            throw new BizException("炉号 " + furnace.getFurnaceCode() + " 处于停炉状态，不允许投料");
        }
        // 投料联锁：存在未恢复的异常工况处置单（含严重告警冻结、关闭但保持冻结）时一律拒绝
        disposalService.assertFeedAllowed(request.getTargetFurnaceCode());
        BigDecimal stock = nvl(pit.getCurrentStockTon());
        if (stock.compareTo(feedTon) < 0) {
            throw new BizException("储坑 " + pit.getPitNo() + " 当前库存 " + stock.setScale(2, RoundingMode.HALF_UP)
                    + " t，不足本次投料 " + feedTon.setScale(2, RoundingMode.HALF_UP) + " t");
        }
        pit.setCurrentStockTon(stock.subtract(feedTon));
        pit.setGrabNo(request.getGrabNo());
        furnace.setDailyProcessTon(nvl(furnace.getDailyProcessTon()).add(feedTon));
        furnace.setGrabNo(request.getGrabNo());
        incineratorRepository.save(furnace);
        refreshStatus(pit);
        return PitPayload.VO.from(pitRepository.save(pit));
    }

    /**
     * 渗滤液抽出登记
     */
    public PitPayload.VO pump(String pitNo, PitPayload.LeachateRequest request) {
        StoragePit pit = requirePit(pitNo);
        BigDecimal pump = request.getPumpM3();
        if (pump == null || pump.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("渗滤液抽出量必须大于 0");
        }
        pit.setLeachatePumpedM3(nvl(pit.getLeachatePumpedM3()).add(pump));
        return PitPayload.VO.from(pitRepository.save(pit));
    }

    /**
     * 堆酵天数与坑温推进（演示用：模拟发酵进程）
     */
    public PitPayload.VO ferment(String pitNo, BigDecimal days, BigDecimal tempC) {
        StoragePit pit = requirePit(pitNo);
        if (days != null) {
            if (days.compareTo(BigDecimal.ZERO) < 0 || days.compareTo(BigDecimal.valueOf(15)) > 0) {
                throw new BizException("堆酵天数应在 0 ~ 15 天之间");
            }
            pit.setFermentationDays(days);
        }
        if (tempC != null) {
            if (tempC.compareTo(BigDecimal.ZERO) < 0 || tempC.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new BizException("坑温应在 0 ~ 90℃ 之间");
            }
            pit.setPitTempC(tempC);
        }
        refreshStatus(pit);
        return PitPayload.VO.from(pitRepository.save(pit));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dict() {
        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("grabNos", Dict.GRAB_NOS);
        dict.put("statuses", Dict.PIT_STATUS.stream()
                .map(code -> (Object) Map.of("code", code, "name", Dict.pitStatusName(code)))
                .toList());
        dict.put("furnaceCodes", incineratorRepository.findAll().stream()
                .map(Incinerator::getFurnaceCode).toList());
        dict.put("fullRate", Dict.PIT_FULL_RATE);
        return dict;
    }

    // ---------------- 内部方法 ----------------

    private StoragePit requirePit(String pitNo) {
        return pitRepository.findByPitNo(pitNo)
                .orElseThrow(() -> new BizException("储坑不存在：" + pitNo));
    }

    private void refreshStatus(StoragePit pit) {
        BigDecimal capacity = nvl(pit.getCapacityTon());
        BigDecimal stock = nvl(pit.getCurrentStockTon());
        double rate = capacity.compareTo(BigDecimal.ZERO) == 0 ? 0D : stock.doubleValue() / capacity.doubleValue();
        if (rate >= 1.0) {
            pit.setStatus("FULL");
        } else if ("OVERHAUL".equals(pit.getStatus())) {
            pit.setStatus("OVERHAUL");
        } else if ("FERMENTING".equals(pit.getStatus()) || rate > 0) {
            pit.setStatus("FERMENTING");
        } else {
            pit.setStatus("NORMAL");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
