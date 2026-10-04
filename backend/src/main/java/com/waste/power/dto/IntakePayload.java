package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.waste.power.common.Dict;
import com.waste.power.domain.IntakeRecord;

/**
 * 进厂计量模块的请求与视图对象。
 */
public class IntakePayload {

    /** 新增/编辑进厂车次的请求体 */
    public static class Request {

        private String truckNo;
        private String sourceRegion;
        private String transferStation;
        private String wasteCategory;
        private BigDecimal grossWeightTon;
        private BigDecimal tareWeightTon;
        private String scaleNo;
        private BigDecimal calorificValue;
        private String driverName;
        private String unloadPit;
        private String remark;
        private LocalDateTime intakeTime;

        public String getTruckNo() {
            return truckNo;
        }

        public void setTruckNo(String truckNo) {
            this.truckNo = truckNo;
        }

        public String getSourceRegion() {
            return sourceRegion;
        }

        public void setSourceRegion(String sourceRegion) {
            this.sourceRegion = sourceRegion;
        }

        public String getTransferStation() {
            return transferStation;
        }

        public void setTransferStation(String transferStation) {
            this.transferStation = transferStation;
        }

        public String getWasteCategory() {
            return wasteCategory;
        }

        public void setWasteCategory(String wasteCategory) {
            this.wasteCategory = wasteCategory;
        }

        public BigDecimal getGrossWeightTon() {
            return grossWeightTon;
        }

        public void setGrossWeightTon(BigDecimal grossWeightTon) {
            this.grossWeightTon = grossWeightTon;
        }

        public BigDecimal getTareWeightTon() {
            return tareWeightTon;
        }

        public void setTareWeightTon(BigDecimal tareWeightTon) {
            this.tareWeightTon = tareWeightTon;
        }

        public String getScaleNo() {
            return scaleNo;
        }

        public void setScaleNo(String scaleNo) {
            this.scaleNo = scaleNo;
        }

        public BigDecimal getCalorificValue() {
            return calorificValue;
        }

        public void setCalorificValue(BigDecimal calorificValue) {
            this.calorificValue = calorificValue;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public String getUnloadPit() {
            return unloadPit;
        }

        public void setUnloadPit(String unloadPit) {
            this.unloadPit = unloadPit;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public LocalDateTime getIntakeTime() {
            return intakeTime;
        }

        public void setIntakeTime(LocalDateTime intakeTime) {
            this.intakeTime = intakeTime;
        }
    }

    /** 地磅流水行视图 */
    public static class VO {

        private Long id;
        private String truckNo;
        private String sourceRegion;
        private String transferStation;
        private String wasteCategory;
        private String wasteCategoryName;
        private BigDecimal grossWeightTon;
        private BigDecimal tareWeightTon;
        private BigDecimal netWeightTon;
        private LocalDateTime intakeTime;
        private String scaleNo;
        private BigDecimal calorificValue;
        private String driverName;
        private String unloadPit;
        private String remark;

        public static VO from(IntakeRecord entity) {
            VO vo = new VO();
            vo.id = entity.getId();
            vo.truckNo = entity.getTruckNo();
            vo.sourceRegion = entity.getSourceRegion();
            vo.transferStation = entity.getTransferStation();
            vo.wasteCategory = entity.getWasteCategory();
            vo.wasteCategoryName = Dict.categoryName(entity.getWasteCategory());
            vo.grossWeightTon = entity.getGrossWeightTon();
            vo.tareWeightTon = entity.getTareWeightTon();
            vo.netWeightTon = entity.getNetWeightTon();
            vo.intakeTime = entity.getIntakeTime();
            vo.scaleNo = entity.getScaleNo();
            vo.calorificValue = entity.getCalorificValue();
            vo.driverName = entity.getDriverName();
            vo.unloadPit = entity.getUnloadPit();
            vo.remark = entity.getRemark();
            return vo;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTruckNo() {
            return truckNo;
        }

        public void setTruckNo(String truckNo) {
            this.truckNo = truckNo;
        }

        public String getSourceRegion() {
            return sourceRegion;
        }

        public void setSourceRegion(String sourceRegion) {
            this.sourceRegion = sourceRegion;
        }

        public String getTransferStation() {
            return transferStation;
        }

        public void setTransferStation(String transferStation) {
            this.transferStation = transferStation;
        }

        public String getWasteCategory() {
            return wasteCategory;
        }

        public void setWasteCategory(String wasteCategory) {
            this.wasteCategory = wasteCategory;
        }

        public String getWasteCategoryName() {
            return wasteCategoryName;
        }

        public void setWasteCategoryName(String wasteCategoryName) {
            this.wasteCategoryName = wasteCategoryName;
        }

        public BigDecimal getGrossWeightTon() {
            return grossWeightTon;
        }

        public void setGrossWeightTon(BigDecimal grossWeightTon) {
            this.grossWeightTon = grossWeightTon;
        }

        public BigDecimal getTareWeightTon() {
            return tareWeightTon;
        }

        public void setTareWeightTon(BigDecimal tareWeightTon) {
            this.tareWeightTon = tareWeightTon;
        }

        public BigDecimal getNetWeightTon() {
            return netWeightTon;
        }

        public void setNetWeightTon(BigDecimal netWeightTon) {
            this.netWeightTon = netWeightTon;
        }

        public LocalDateTime getIntakeTime() {
            return intakeTime;
        }

        public void setIntakeTime(LocalDateTime intakeTime) {
            this.intakeTime = intakeTime;
        }

        public String getScaleNo() {
            return scaleNo;
        }

        public void setScaleNo(String scaleNo) {
            this.scaleNo = scaleNo;
        }

        public BigDecimal getCalorificValue() {
            return calorificValue;
        }

        public void setCalorificValue(BigDecimal calorificValue) {
            this.calorificValue = calorificValue;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public String getUnloadPit() {
            return unloadPit;
        }

        public void setUnloadPit(String unloadPit) {
            this.unloadPit = unloadPit;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
