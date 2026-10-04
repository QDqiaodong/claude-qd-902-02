package com.waste.power.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.waste.power.common.Dict;
import com.waste.power.domain.AshResidue;

/**
 * 灰渣与渗滤液台账的请求与视图对象。
 */
public class AshPayload {

    /** 当日灰渣与渗滤液登记请求 */
    public static class Request {

        private LocalDate recordDate;
        private BigDecimal flyAshTon;
        private BigDecimal bottomAshTon;
        private BigDecimal solidifiedTon;
        private String disposalDest;
        private BigDecimal leachateTreatedM3;
        private BigDecimal effluentCod;
        private String shift;
        private String remark;

        public LocalDate getRecordDate() {
            return recordDate;
        }

        public void setRecordDate(LocalDate recordDate) {
            this.recordDate = recordDate;
        }

        public BigDecimal getFlyAshTon() {
            return flyAshTon;
        }

        public void setFlyAshTon(BigDecimal flyAshTon) {
            this.flyAshTon = flyAshTon;
        }

        public BigDecimal getBottomAshTon() {
            return bottomAshTon;
        }

        public void setBottomAshTon(BigDecimal bottomAshTon) {
            this.bottomAshTon = bottomAshTon;
        }

        public BigDecimal getSolidifiedTon() {
            return solidifiedTon;
        }

        public void setSolidifiedTon(BigDecimal solidifiedTon) {
            this.solidifiedTon = solidifiedTon;
        }

        public String getDisposalDest() {
            return disposalDest;
        }

        public void setDisposalDest(String disposalDest) {
            this.disposalDest = disposalDest;
        }

        public BigDecimal getLeachateTreatedM3() {
            return leachateTreatedM3;
        }

        public void setLeachateTreatedM3(BigDecimal leachateTreatedM3) {
            this.leachateTreatedM3 = leachateTreatedM3;
        }

        public BigDecimal getEffluentCod() {
            return effluentCod;
        }

        public void setEffluentCod(BigDecimal effluentCod) {
            this.effluentCod = effluentCod;
        }

        public String getShift() {
            return shift;
        }

        public void setShift(String shift) {
            this.shift = shift;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /** 台账视图 */
    public static class VO {

        private Long id;
        private LocalDate recordDate;
        private BigDecimal flyAshTon;
        private BigDecimal bottomAshTon;
        private BigDecimal solidifiedTon;
        private String disposalDest;
        private BigDecimal leachateTreatedM3;
        private BigDecimal effluentCod;
        private Boolean effluentOk;
        private String shift;
        private String remark;
        /** 炉渣/飞灰 热灼减率考核口径：飞灰占入炉量比例 */
        private Double flyAshRate;

        public static VO from(AshResidue record) {
            VO vo = new VO();
            vo.id = record.getId();
            vo.recordDate = record.getRecordDate();
            vo.flyAshTon = record.getFlyAshTon();
            vo.bottomAshTon = record.getBottomAshTon();
            vo.solidifiedTon = record.getSolidifiedTon();
            vo.disposalDest = record.getDisposalDest();
            vo.leachateTreatedM3 = record.getLeachateTreatedM3();
            vo.effluentCod = record.getEffluentCod();
            vo.effluentOk = record.getEffluentOk();
            vo.shift = record.getShift();
            vo.remark = record.getRemark();
            BigDecimal ash = sum(record.getFlyAshTon(), record.getBottomAshTon());
            if (ash.compareTo(BigDecimal.ZERO) > 0 && record.getFlyAshTon() != null) {
                vo.flyAshRate = record.getFlyAshTon().doubleValue() / ash.doubleValue();
            } else {
                vo.flyAshRate = 0D;
            }
            return vo;
        }

        private static BigDecimal sum(BigDecimal a, BigDecimal b) {
            BigDecimal x = a == null ? BigDecimal.ZERO : a;
            BigDecimal y = b == null ? BigDecimal.ZERO : b;
            return x.add(y);
        }

        public static boolean effluentQualified(BigDecimal cod) {
            return cod != null && cod.compareTo(Dict.EFFLUENT_COD_LIMIT) <= 0;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LocalDate getRecordDate() {
            return recordDate;
        }

        public void setRecordDate(LocalDate recordDate) {
            this.recordDate = recordDate;
        }

        public BigDecimal getFlyAshTon() {
            return flyAshTon;
        }

        public void setFlyAshTon(BigDecimal flyAshTon) {
            this.flyAshTon = flyAshTon;
        }

        public BigDecimal getBottomAshTon() {
            return bottomAshTon;
        }

        public void setBottomAshTon(BigDecimal bottomAshTon) {
            this.bottomAshTon = bottomAshTon;
        }

        public BigDecimal getSolidifiedTon() {
            return solidifiedTon;
        }

        public void setSolidifiedTon(BigDecimal solidifiedTon) {
            this.solidifiedTon = solidifiedTon;
        }

        public String getDisposalDest() {
            return disposalDest;
        }

        public void setDisposalDest(String disposalDest) {
            this.disposalDest = disposalDest;
        }

        public BigDecimal getLeachateTreatedM3() {
            return leachateTreatedM3;
        }

        public void setLeachateTreatedM3(BigDecimal leachateTreatedM3) {
            this.leachateTreatedM3 = leachateTreatedM3;
        }

        public BigDecimal getEffluentCod() {
            return effluentCod;
        }

        public void setEffluentCod(BigDecimal effluentCod) {
            this.effluentCod = effluentCod;
        }

        public Boolean getEffluentOk() {
            return effluentOk;
        }

        public void setEffluentOk(Boolean effluentOk) {
            this.effluentOk = effluentOk;
        }

        public String getShift() {
            return shift;
        }

        public void setShift(String shift) {
            this.shift = shift;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Double getFlyAshRate() {
            return flyAshRate;
        }

        public void setFlyAshRate(Double flyAshRate) {
            this.flyAshRate = flyAshRate;
        }
    }
}
