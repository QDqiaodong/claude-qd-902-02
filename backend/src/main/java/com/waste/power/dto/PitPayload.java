package com.waste.power.dto;

import java.math.BigDecimal;

import com.waste.power.common.Dict;
import com.waste.power.domain.StoragePit;

/**
 * 垃圾储坑模块的请求与视图对象（含抓斗投料、渗滤液抽出）。
 */
public class PitPayload {

    /** 抓斗投料请求 */
    public static class FeedRequest {

        private String grabNo;
        private BigDecimal feedTon;
        private String targetFurnaceCode;

        public String getGrabNo() {
            return grabNo;
        }

        public void setGrabNo(String grabNo) {
            this.grabNo = grabNo;
        }

        public BigDecimal getFeedTon() {
            return feedTon;
        }

        public void setFeedTon(BigDecimal feedTon) {
            this.feedTon = feedTon;
        }

        public String getTargetFurnaceCode() {
            return targetFurnaceCode;
        }

        public void setTargetFurnaceCode(String targetFurnaceCode) {
            this.targetFurnaceCode = targetFurnaceCode;
        }
    }

    /** 卸料入坑请求 */
    public static class UnloadRequest {

        private BigDecimal unloadTon;
        private String wasteCategory;
        private String truckNo;

        public BigDecimal getUnloadTon() {
            return unloadTon;
        }

        public void setUnloadTon(BigDecimal unloadTon) {
            this.unloadTon = unloadTon;
        }

        public String getWasteCategory() {
            return wasteCategory;
        }

        public void setWasteCategory(String wasteCategory) {
            this.wasteCategory = wasteCategory;
        }

        public String getTruckNo() {
            return truckNo;
        }

        public void setTruckNo(String truckNo) {
            this.truckNo = truckNo;
        }
    }

    /** 渗滤液抽出登记 */
    public static class LeachateRequest {

        private BigDecimal pumpM3;

        public BigDecimal getPumpM3() {
            return pumpM3;
        }

        public void setPumpM3(BigDecimal pumpM3) {
            this.pumpM3 = pumpM3;
        }
    }

    /** 坑位视图（含库存率与可容纳余量） */
    public static class VO {

        private Long id;
        private String pitNo;
        private String pitName;
        private BigDecimal capacityTon;
        private BigDecimal currentStockTon;
        private BigDecimal fermentationDays;
        private BigDecimal leachatePumpedM3;
        private BigDecimal pitTempC;
        private String grabNo;
        private String status;
        private String statusName;
        private String remark;
        private Double fillRate;
        private BigDecimal freeTon;
        private Boolean nearFull;

        public static VO from(StoragePit pit) {
            VO vo = new VO();
            vo.id = pit.getId();
            vo.pitNo = pit.getPitNo();
            vo.pitName = pit.getPitName();
            vo.capacityTon = pit.getCapacityTon();
            vo.currentStockTon = pit.getCurrentStockTon();
            vo.fermentationDays = pit.getFermentationDays();
            vo.leachatePumpedM3 = pit.getLeachatePumpedM3();
            vo.pitTempC = pit.getPitTempC();
            vo.grabNo = pit.getGrabNo();
            vo.status = pit.getStatus();
            vo.statusName = Dict.pitStatusName(pit.getStatus());
            vo.remark = pit.getRemark();
            BigDecimal capacity = pit.getCapacityTon() == null ? BigDecimal.ZERO : pit.getCapacityTon();
            BigDecimal stock = pit.getCurrentStockTon() == null ? BigDecimal.ZERO : pit.getCurrentStockTon();
            vo.fillRate = capacity.compareTo(BigDecimal.ZERO) == 0 ? 0D
                    : stock.doubleValue() / capacity.doubleValue();
            BigDecimal free = capacity.subtract(stock);
            vo.freeTon = free.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : free;
            vo.nearFull = vo.fillRate >= Dict.PIT_FULL_RATE.doubleValue();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPitNo() {
            return pitNo;
        }

        public void setPitNo(String pitNo) {
            this.pitNo = pitNo;
        }

        public String getPitName() {
            return pitName;
        }

        public void setPitName(String pitName) {
            this.pitName = pitName;
        }

        public BigDecimal getCapacityTon() {
            return capacityTon;
        }

        public void setCapacityTon(BigDecimal capacityTon) {
            this.capacityTon = capacityTon;
        }

        public BigDecimal getCurrentStockTon() {
            return currentStockTon;
        }

        public void setCurrentStockTon(BigDecimal currentStockTon) {
            this.currentStockTon = currentStockTon;
        }

        public BigDecimal getFermentationDays() {
            return fermentationDays;
        }

        public void setFermentationDays(BigDecimal fermentationDays) {
            this.fermentationDays = fermentationDays;
        }

        public BigDecimal getLeachatePumpedM3() {
            return leachatePumpedM3;
        }

        public void setLeachatePumpedM3(BigDecimal leachatePumpedM3) {
            this.leachatePumpedM3 = leachatePumpedM3;
        }

        public BigDecimal getPitTempC() {
            return pitTempC;
        }

        public void setPitTempC(BigDecimal pitTempC) {
            this.pitTempC = pitTempC;
        }

        public String getGrabNo() {
            return grabNo;
        }

        public void setGrabNo(String grabNo) {
            this.grabNo = grabNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusName() {
            return statusName;
        }

        public void setStatusName(String statusName) {
            this.statusName = statusName;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getFillRate() {
            return fillRate;
        }

        public void setFillRate(Double fillRate) {
            this.fillRate = fillRate;
        }

        public BigDecimal getFreeTon() {
            return freeTon;
        }

        public void setFreeTon(BigDecimal freeTon) {
            this.freeTon = freeTon;
        }

        public Boolean getNearFull() {
            return nearFull;
        }

        public void setNearFull(Boolean nearFull) {
            this.nearFull = nearFull;
        }
    }
}
