-- 生活垃圾焚烧发电厂 · 进厂与焚烧运行管理
-- 库：waste_power（ddl-auto: none，表全部由此脚本建立）
-- 主线：垃圾进厂计重 → 储坑发酵 → 焚烧炉 → 余热锅炉发电 → 烟气净化 → 灰渣与渗滤液

CREATE DATABASE IF NOT EXISTS `waste_power`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `waste_power`;

SET NAMES utf8mb4;

-- ---------------- 1. 进厂车次与地磅计量 ----------------
DROP TABLE IF EXISTS `intake_record`;
CREATE TABLE `intake_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `truck_no`         VARCHAR(32)  NOT NULL COMMENT '车牌号',
  `source_region`    VARCHAR(32)  NOT NULL COMMENT '来源行政区划',
  `transfer_station` VARCHAR(64)  DEFAULT NULL COMMENT '来源中转站/压缩站',
  `waste_category`   VARCHAR(16)  NOT NULL COMMENT '垃圾类别 KITCHEN/OTHER/RECYCLABLE/HAZARDOUS',
  `gross_weight_ton` DECIMAL(10,2) DEFAULT NULL COMMENT '毛重 t',
  `tare_weight_ton`  DECIMAL(10,2) DEFAULT NULL COMMENT '皮重 t',
  `net_weight_ton`   DECIMAL(10,2) DEFAULT NULL COMMENT '净重 t = 毛重 − 皮重',
  `intake_time`      DATETIME     DEFAULT NULL COMMENT '进厂时间',
  `scale_no`         VARCHAR(16)  DEFAULT NULL COMMENT '地磅号',
  `calorific_value`  DECIMAL(10,0) DEFAULT NULL COMMENT '低位热值 kJ/kg',
  `driver_name`      VARCHAR(32)  DEFAULT NULL COMMENT '驾驶员',
  `unload_pit`       VARCHAR(16)  DEFAULT NULL COMMENT '卸料坑位',
  `remark`           VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_intake_time` (`intake_time`),
  KEY `idx_intake_region` (`source_region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进厂车次与地磅计量';

-- ---------------- 2. 垃圾储坑 ----------------
DROP TABLE IF EXISTS `storage_pit`;
CREATE TABLE `storage_pit` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `pit_no`             VARCHAR(16)  NOT NULL COMMENT '坑位编号',
  `pit_name`           VARCHAR(64)  DEFAULT NULL COMMENT '坑位名称',
  `capacity_ton`       DECIMAL(10,2) DEFAULT NULL COMMENT '坑容 t',
  `current_stock_ton`  DECIMAL(10,2) DEFAULT NULL COMMENT '当前库存 t',
  `fermentation_days`  DECIMAL(5,1) DEFAULT NULL COMMENT '堆酵天数 d',
  `leachate_pumped_m3` DECIMAL(10,2) DEFAULT NULL COMMENT '渗滤液累积抽出量 m³',
  `pit_temp_c`         DECIMAL(6,1) DEFAULT NULL COMMENT '坑温 ℃',
  `grab_no`            VARCHAR(16)  DEFAULT NULL COMMENT '投料抓斗编号',
  `status`             VARCHAR(16)  DEFAULT NULL COMMENT '状态 FERMENTING/NORMAL/FULL/OVERHAUL',
  `remark`             VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pit_no` (`pit_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='垃圾储坑';

-- ---------------- 3. 焚烧炉（单表继承：炉排炉 / 流化床 / 回转窑，子类由判别公式判定） ----------------
DROP TABLE IF EXISTS `incinerator`;
CREATE TABLE `incinerator` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `furnace_code`           VARCHAR(32)  NOT NULL COMMENT '炉号',
  `furnace_name`           VARCHAR(64)  DEFAULT NULL COMMENT '炉名称',
  `furnace_type_code`      VARCHAR(16)  DEFAULT NULL COMMENT '炉型代码 GRATE/FLUIDIZED/ROTARY（判别公式输入）',
  `rated_capacity_tpd`     DECIMAL(10,2) DEFAULT NULL COMMENT '额定处理量 t/d',
  `furnace_temp_c`         DECIMAL(8,1) DEFAULT NULL COMMENT '炉膛温度 ℃',
  `flue_gas_residence_s`   DECIMAL(6,2) DEFAULT NULL COMMENT '烟气停留时间 s',
  `co_ppm`                 DECIMAL(8,2) DEFAULT NULL COMMENT 'CO 浓度 ppm',
  `combustion_status`      VARCHAR(16)  DEFAULT NULL COMMENT '燃烧状态 STARTING/STABLE/BANKING/SHUTDOWN/FAULT',
  `daily_process_ton`      DECIMAL(10,2) DEFAULT NULL COMMENT '当日入炉量 t',
  `running_hours`          DECIMAL(10,1) DEFAULT NULL COMMENT '累计运行小时 h',
  `last_start_time`        DATETIME     DEFAULT NULL COMMENT '最近启炉时间',
  `grab_no`                VARCHAR(16)  DEFAULT NULL COMMENT '供料抓斗',
  `stack_code`             VARCHAR(32)  DEFAULT NULL COMMENT '对应排放口编号（同炉排放告警据此归集）',
  `remark`                 VARCHAR(128) DEFAULT NULL,
  -- 子类：炉排炉 GrateFurnace
  `grate_speed_mh`         DECIMAL(6,2) DEFAULT NULL COMMENT '炉排速度 m/h',
  `grate_bed_thickness_mm` DECIMAL(8,1) DEFAULT NULL COMMENT '料层厚度 mm',
  `grate_stage_count`      INT          DEFAULT NULL COMMENT '炉排段数',
  `primary_air_temp_c`     DECIMAL(6,1) DEFAULT NULL COMMENT '一次风温 ℃',
  -- 子类：流化床 FluidizedBed
  `bed_material_height_mm` DECIMAL(8,1) DEFAULT NULL COMMENT '床料高度 mm',
  `fluidizing_air_speed_ms` DECIMAL(6,2) DEFAULT NULL COMMENT '流化风速 m/s',
  `bed_temp_c`             DECIMAL(6,1) DEFAULT NULL COMMENT '床温 ℃',
  `bed_material_size_mm`   DECIMAL(6,2) DEFAULT NULL COMMENT '床料粒径 mm',
  -- 子类：回转窑 RotaryKiln
  `kiln_speed_rpm`         DECIMAL(6,2) DEFAULT NULL COMMENT '窑体转速 r/min',
  `kiln_inclination_deg`   DECIMAL(5,2) DEFAULT NULL COMMENT '窑体倾角 °',
  `kiln_length_m`          DECIMAL(6,2) DEFAULT NULL COMMENT '窑长 m',
  `kiln_tail_temp_c`       DECIMAL(6,1) DEFAULT NULL COMMENT '窑尾温度 ℃',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_furnace_code` (`furnace_code`),
  KEY `idx_furnace_type` (`furnace_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='焚烧炉（单表继承三炉型）';

-- ---------------- 4. 启停炉与运行记录 ----------------
DROP TABLE IF EXISTS `furnace_run_log`;
CREATE TABLE `furnace_run_log` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `furnace_code`  VARCHAR(32)  NOT NULL COMMENT '炉号',
  `action`        VARCHAR(16)  NOT NULL COMMENT '动作 START/STABILIZE/STOP',
  `operate_time`  DATETIME     DEFAULT NULL COMMENT '操作时间',
  `period_month`  VARCHAR(7)   DEFAULT NULL COMMENT '所属月份 yyyy-MM',
  `furnace_temp_c` DECIMAL(8,1) DEFAULT NULL COMMENT '炉膛温度 ℃',
  `process_ton`   DECIMAL(10,2) DEFAULT NULL COMMENT '当班处理量 t',
  `power_kwh`     DECIMAL(14,2) DEFAULT NULL COMMENT '当班发电量 kWh',
  `run_hours`     DECIMAL(8,1) DEFAULT NULL COMMENT '当班运行小时 h',
  `operator`      VARCHAR(32)  DEFAULT NULL COMMENT '操作员',
  `remark`        VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_runlog_furnace` (`furnace_code`, `period_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='焚烧炉启停炉与运行记录';

-- ---------------- 5. 余热锅炉 ----------------
DROP TABLE IF EXISTS `boiler`;
CREATE TABLE `boiler` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `boiler_no`             VARCHAR(32)  NOT NULL COMMENT '锅炉编号',
  `boiler_name`           VARCHAR(64)  DEFAULT NULL COMMENT '锅炉名称',
  `furnace_code`          VARCHAR(32)  DEFAULT NULL COMMENT '对应焚烧炉炉号',
  `steam_pressure_mpa`    DECIMAL(6,2) DEFAULT NULL COMMENT '蒸汽压力 MPa',
  `steam_temp_c`          DECIMAL(6,1) DEFAULT NULL COMMENT '蒸汽温度 ℃',
  `evaporation_tph`       DECIMAL(8,2) DEFAULT NULL COMMENT '蒸发量 t/h',
  `feed_water_temp_c`     DECIMAL(6,1) DEFAULT NULL COMMENT '给水温度 ℃',
  `flue_gas_temp_c`       DECIMAL(6,1) DEFAULT NULL COMMENT '排烟温度 ℃',
  `rated_evaporation_tph` DECIMAL(8,2) DEFAULT NULL COMMENT '额定蒸发量 t/h',
  `status`                VARCHAR(16)  DEFAULT NULL COMMENT '状态 RUNNING/BANKING/OVERHAUL',
  `remark`                VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_boiler_no` (`boiler_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='余热锅炉';

-- ---------------- 6. 汽轮发电机组 ----------------
DROP TABLE IF EXISTS `turbine_generator`;
CREATE TABLE `turbine_generator` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `unit_no`        VARCHAR(32)  NOT NULL COMMENT '机组编号',
  `unit_name`      VARCHAR(64)  DEFAULT NULL COMMENT '机组名称',
  `capacity_mw`    DECIMAL(8,2) DEFAULT NULL COMMENT '装机容量 MW',
  `generation_kwh` DECIMAL(14,2) DEFAULT NULL COMMENT '最近统计日发电增量 kWh（日结口径，非累计表码）',
  `grid_kwh`       DECIMAL(14,2) DEFAULT NULL COMMENT '最近统计日上网电量 kWh（日结口径）',
  `auxiliary_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '最近统计日厂用电率 %',
  `running_hours`  DECIMAL(10,1) DEFAULT NULL COMMENT '最近统计日运行小时 h',
  `main_meter_reading_kwh` DECIMAL(16,2) DEFAULT NULL COMMENT '主电能表当前累计表码（期末抄见）kWh',
  `grid_meter_reading_kwh` DECIMAL(16,2) DEFAULT NULL COMMENT '上网关口表当前累计表码 kWh',
  `meter_no`       VARCHAR(32) DEFAULT NULL COMMENT '当前主电能表表号',
  `basis_source`   VARCHAR(32) DEFAULT NULL COMMENT '当前增量基数来源 PREV_DAY/INIT_BASE/NEW_METER_BASE/VERIFIED_BASE',
  `stat_date`      DATE         DEFAULT NULL COMMENT '最近统计日期',
  `status`         VARCHAR(16)  DEFAULT NULL COMMENT '状态 ON_GRID/IDLE/OVERHAUL',
  `remark`         VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_no` (`unit_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='汽轮发电机组';

-- ---------------- 6b. 汽轮发电机组发电日结（按机组 + 统计日 + 相邻表码计算当日增量） ----------------
DROP TABLE IF EXISTS `power_daily_settlement`;
CREATE TABLE `power_daily_settlement` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `unit_no`        VARCHAR(32)  NOT NULL COMMENT '机组编号',
  `stat_date`      DATE         NOT NULL COMMENT '统计日',
  `status`         VARCHAR(16)  NOT NULL COMMENT '日结状态 SETTLED 已日结 / SUSPENDED 暂停待校表 / VERIFIED 已校表待日结',
  -- 主电能表表码与当日增量
  `main_meter_no`            VARCHAR(32)  DEFAULT NULL COMMENT '当日主电能表表号',
  `prev_reading_kwh`         DECIMAL(16,2) DEFAULT NULL COMMENT '期初（相邻）表码 kWh',
  `current_reading_kwh`      DECIMAL(16,2) DEFAULT NULL COMMENT '期末（当日抄见）表码 kWh',
  `basis_source`             VARCHAR(32)  NOT NULL COMMENT '增量基数来源 PREV_DAY 相邻日结 / INIT_BASE 首次建档 / NEW_METER_BASE 换表新基数 / VERIFIED_BASE 校表确认',
  `generation_kwh`           DECIMAL(14,2) DEFAULT NULL COMMENT '当日发电增量 = 期末表码 − 期初基数 kWh',
  -- 上网关口表表码与当日上网电量
  `grid_meter_no`            VARCHAR(32)  DEFAULT NULL COMMENT '上网关口表表号',
  `grid_prev_reading_kwh`    DECIMAL(16,2) DEFAULT NULL COMMENT '上网表期初表码 kWh',
  `grid_current_reading_kwh` DECIMAL(16,2) DEFAULT NULL COMMENT '上网表期末表码 kWh',
  `grid_kwh`                 DECIMAL(14,2) DEFAULT NULL COMMENT '当日上网电量 kWh',
  `auxiliary_rate`           DECIMAL(5,2)  DEFAULT NULL COMMENT '厂用电率 % = (发电增量−上网电量)/发电增量',
  `running_hours`            DECIMAL(10,1) DEFAULT NULL COMMENT '当日运行小时 h',
  -- 表计回拨 / 更换的明确处置（二选一，全程留痕）
  `meter_event`              VARCHAR(32)  DEFAULT NULL COMMENT '表计事件 NONE/ROLLBACK 回拨/METER_REPLACED 更换',
  `anomaly_status`           VARCHAR(32)  NOT NULL DEFAULT 'NORMAL' COMMENT '异常状态 NORMAL/METER_ROLLBACK/METER_REPLACED/SUSPENDED',
  `old_meter_sealed_reading_kwh` DECIMAL(16,2) DEFAULT NULL COMMENT '旧表封账表码 kWh',
  `new_meter_no`             VARCHAR(32)  DEFAULT NULL COMMENT '新表表号',
  `new_meter_base_reading_kwh` DECIMAL(16,2) DEFAULT NULL COMMENT '新表启用基数 kWh（后续增量口径锚点）',
  `decision`                 VARCHAR(32)  DEFAULT NULL COMMENT '异常处置选择 SEAL_NEW_BASE 旧表封账启用新基数 / SUSPEND 暂停日结等待校表',
  `decision_reason`          VARCHAR(255) DEFAULT NULL COMMENT '处置理由',
  `decided_by`               VARCHAR(32)  DEFAULT NULL COMMENT '处置决策人',
  `decided_time`             DATETIME     DEFAULT NULL COMMENT '处置时间',
  -- 暂停日结后的校表登记
  `verified_reading_kwh`     DECIMAL(16,2) DEFAULT NULL COMMENT '校表确认的有效基数 kWh',
  `verified_by`              VARCHAR(32)  DEFAULT NULL COMMENT '校表人',
  `verified_time`            DATETIME     DEFAULT NULL COMMENT '校表时间',
  `verify_note`              VARCHAR(255) DEFAULT NULL COMMENT '校表说明',
  -- 审计与并发控制
  `operator`       VARCHAR(32)  DEFAULT NULL COMMENT '日结操作人',
  `remark`         VARCHAR(255) DEFAULT NULL,
  `request_fingerprint` VARCHAR(64) DEFAULT NULL COMMENT '请求幂等指纹（重复请求直接返回原日结）',
  `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁：过期页面/重复校表提交拦截',
  `settle_time`    DATETIME     DEFAULT NULL COMMENT '日结完成时间',
  `create_time`    DATETIME     NOT NULL,
  `update_time`    DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_stat_date` (`unit_no`, `stat_date`),
  KEY `idx_settlement_unit_date` (`unit_no`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='汽轮发电机组发电日结';

-- ---------------- 6c. 发电日结 / 表计处置操作轨迹（回拨、换表、校表、重复请求拒绝全部留痕） ----------------
DROP TABLE IF EXISTS `power_meter_trace`;
CREATE TABLE `power_meter_trace` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `unit_no`       VARCHAR(32)  NOT NULL COMMENT '机组编号',
  `stat_date`     DATE         DEFAULT NULL COMMENT '统计日',
  `settlement_id` BIGINT       DEFAULT NULL COMMENT '日结单 id',
  `action`        VARCHAR(40)  NOT NULL COMMENT '动作 SETTLE/IDEMPOTENT_RETRY/REJECT_DUPLICATE/REJECT_STALE_DATE/SEAL_NEW_BASE/SUSPEND/VERIFY/FAIL',
  `from_status`   VARCHAR(16)  DEFAULT NULL,
  `to_status`     VARCHAR(16)  DEFAULT NULL,
  `operator_name` VARCHAR(32)  DEFAULT NULL COMMENT '操作人',
  `result`        VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '结果 SUCCESS/FAILED',
  `detail`        VARCHAR(512) DEFAULT NULL COMMENT '计算依据或失败原因',
  `operate_time`  DATETIME     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_meter_trace_settlement` (`settlement_id`),
  KEY `idx_meter_trace_unit` (`unit_no`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发电日结与表计处置操作轨迹';

-- ---------------- 7. 烟气排放指标 ----------------
DROP TABLE IF EXISTS `emission_record`;
CREATE TABLE `emission_record` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `indicator_code` VARCHAR(16)  NOT NULL COMMENT '指标代码 PARTICULATE/SO2/NOX/HCL/CO/DIOXIN',
  `indicator_name` VARCHAR(32)  DEFAULT NULL COMMENT '指标名称',
  `measured_value` DECIMAL(12,3) DEFAULT NULL COMMENT '实测值',
  `limit_value`    DECIMAL(12,3) DEFAULT NULL COMMENT '限值',
  `unit`           VARCHAR(16)  DEFAULT NULL COMMENT '单位',
  `exceeded`       TINYINT(1)   DEFAULT 0 COMMENT '是否超标',
  `exceed_ratio`   DECIMAL(8,2) DEFAULT NULL COMMENT '超标倍率 %',
  `period_label`   VARCHAR(64)  DEFAULT NULL COMMENT '监测时段',
  `monitor_time`   DATETIME     DEFAULT NULL COMMENT '监测时间',
  `stack_code`     VARCHAR(32)  DEFAULT NULL COMMENT '排放口/对应炉号',
  `emission_flag`  VARCHAR(16)  DEFAULT NULL COMMENT '排放标记 NORMAL/EXCEED',
  `remark`         VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_emission_indicator` (`indicator_code`, `monitor_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烟气排放指标';

-- ---------------- 8. 环保告警 ----------------
DROP TABLE IF EXISTS `env_alarm`;
CREATE TABLE `env_alarm` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `alarm_no`       VARCHAR(32)  DEFAULT NULL COMMENT '告警编号',
  `indicator_code` VARCHAR(16)  DEFAULT NULL COMMENT '指标代码',
  `indicator_name` VARCHAR(32)  DEFAULT NULL COMMENT '指标名称',
  `measured_value` DECIMAL(12,3) DEFAULT NULL COMMENT '实测值',
  `limit_value`    DECIMAL(12,3) DEFAULT NULL COMMENT '限值',
  `exceed_ratio`   DECIMAL(8,2) DEFAULT NULL COMMENT '超标倍率 %',
  `alarm_level`    VARCHAR(16)  DEFAULT NULL COMMENT '等级 MINOR/MAJOR/SEVERE',
  `period_label`   VARCHAR(64)  DEFAULT NULL COMMENT '超标时段',
  `stack_code`     VARCHAR(32)  DEFAULT NULL COMMENT '排放口',
  `alarm_time`     DATETIME     DEFAULT NULL COMMENT '告警时间',
  `handled`        TINYINT(1)   DEFAULT 0 COMMENT '是否已处理',
  `handle_note`    VARCHAR(128) DEFAULT NULL COMMENT '处理说明',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alarm_no` (`alarm_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='环保告警';

-- ---------------- 9. 灰渣与渗滤液台账 ----------------
DROP TABLE IF EXISTS `ash_residue`;
CREATE TABLE `ash_residue` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
  `record_date`         DATE         DEFAULT NULL COMMENT '台账日期',
  `fly_ash_ton`         DECIMAL(10,2) DEFAULT NULL COMMENT '飞灰产生量 t',
  `bottom_ash_ton`      DECIMAL(10,2) DEFAULT NULL COMMENT '炉渣产生量 t',
  `solidified_ton`      DECIMAL(10,2) DEFAULT NULL COMMENT '飞灰固化量 t',
  `disposal_dest`       VARCHAR(64)  DEFAULT NULL COMMENT '固化后去向',
  `leachate_treated_m3` DECIMAL(10,2) DEFAULT NULL COMMENT '渗滤液处理量 m³',
  `effluent_cod`        DECIMAL(8,2) DEFAULT NULL COMMENT '出水 COD mg/L',
  `effluent_ok`         TINYINT(1)   DEFAULT 1 COMMENT '出水是否达标',
  `shift`               VARCHAR(16)  DEFAULT NULL COMMENT '班次',
  `remark`              VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ash_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='灰渣与渗滤液台账';


-- ---------------- 10. 异常工况处置单（炉况判定 / 同炉排放告警 / 储坑抓斗快照 → 冻结投料 → 复核恢复 闭环） ----------------
DROP TABLE IF EXISTS `abnormal_disposal`;
CREATE TABLE `abnormal_disposal` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `disposal_no`            VARCHAR(40)  NOT NULL COMMENT '处置单编号',
  `furnace_code`           VARCHAR(32)  NOT NULL COMMENT '炉号',
  `furnace_name`           VARCHAR(64)  DEFAULT NULL COMMENT '炉名称（快照）',
  `status`                 VARCHAR(16)  NOT NULL COMMENT '状态 PENDING/HANDLING/REVIEWING/RECOVERED/CLOSED',
  `active_once`           VARCHAR(32)  GENERATED ALWAYS AS (CASE WHEN `status` = 'CLOSED' THEN NULL ELSE `furnace_code` END) STORED COMMENT '未关闭处置单同炉唯一占用（关闭后为 NULL，不占名额）',
  `trigger_source`         VARCHAR(16)  NOT NULL COMMENT '首发来源 AUTO/MANUAL',
  `trigger_reasons`        VARCHAR(512) DEFAULT NULL COMMENT '触发原因（不达标项等，JSON 数组）',
  -- 炉况快照（建立时冻结，不改动任何已完成的运行记录）
  `snapshot_furnace_temp_c`  DECIMAL(8,1)  DEFAULT NULL COMMENT '快照炉膛温度 ℃',
  `snapshot_residence_s`     DECIMAL(6,2)  DEFAULT NULL COMMENT '快照烟气停留 s',
  `snapshot_co_ppm`          DECIMAL(8,2)  DEFAULT NULL COMMENT '快照 CO ppm',
  `snapshot_combustion`      VARCHAR(16)   DEFAULT NULL COMMENT '快照燃烧状态',
  `snapshot_daily_ton`       DECIMAL(10,2) DEFAULT NULL COMMENT '快照当日入炉量 t',
  `snapshot_stack_code`      VARCHAR(32)   DEFAULT NULL COMMENT '快照排放口',
  -- 供料抓斗快照（炉侧抓斗 + 最近向该炉供料的坑位抓斗）
  `snapshot_grab_no`         VARCHAR(16)   DEFAULT NULL COMMENT '炉侧供料抓斗',
  `snapshot_pit_no`          VARCHAR(16)   DEFAULT NULL COMMENT '最近供料坑位',
  `snapshot_pit_grab_no`     VARCHAR(16)   DEFAULT NULL COMMENT '坑位抓斗编号',
  `snapshot_pit_stock_ton`   DECIMAL(10,2) DEFAULT NULL COMMENT '快照坑位库存 t',
  -- 投料联锁
  `feed_frozen`            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否冻结投料',
  `frozen_time`            DATETIME     DEFAULT NULL COMMENT '投料冻结时间',
  `unfrozen_time`          DATETIME     DEFAULT NULL COMMENT '投料解冻时间',
  -- 人工处置
  `owner_name`             VARCHAR(32)  DEFAULT NULL COMMENT '处置负责人',
  `measure_note`           VARCHAR(512) DEFAULT NULL COMMENT '处置措施记录',
  `handled_time`           DATETIME     DEFAULT NULL COMMENT '开始处置时间',
  `review_request_time`    DATETIME     DEFAULT NULL COMMENT '申请复核时间',
  -- 复核恢复（必须由另一名复核人员执行）
  `reviewer_name`          VARCHAR(32)  DEFAULT NULL COMMENT '复核人员（不得与负责人为同一人）',
  `review_note`            VARCHAR(512) DEFAULT NULL COMMENT '复核意见',
  `recover_furnace_temp_c` DECIMAL(8,1) DEFAULT NULL COMMENT '恢复时炉膛温度 ℃',
  `recover_residence_s`    DECIMAL(6,2) DEFAULT NULL COMMENT '恢复时烟气停留 s',
  -- 告警数据暂不可用时的明确选择
  `alarm_data_status`      VARCHAR(16)  NOT NULL DEFAULT 'AVAILABLE' COMMENT '告警数据 AVAILABLE/UNAVAILABLE',
  `missing_choice`         VARCHAR(32)  DEFAULT NULL COMMENT '缺失选择 WAIT_FREEZE/CLOSE_KEEP_FROZEN',
  `missing_reason`         VARCHAR(255) DEFAULT NULL COMMENT '数据缺失处置理由',
  `missing_decided_by`     VARCHAR(32)  DEFAULT NULL COMMENT '缺失选择决策人',
  `missing_decided_time`   DATETIME     DEFAULT NULL COMMENT '缺失选择时间',
  -- 乐观锁：重复点击 / 过期版本提交 / 两人同时复核 防误放行
  `version`                INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `close_time`             DATETIME     DEFAULT NULL COMMENT '关闭时间',
  `remark`                 VARCHAR(255) DEFAULT NULL,
  `create_time`            DATETIME     NOT NULL COMMENT '创建时间',
  `update_time`            DATETIME     NOT NULL COMMENT '最近更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_disposal_no` (`disposal_no`),
  -- 同炉未恢复（未关闭）前只能存在一张有效处置单：关闭态不占名额
  UNIQUE KEY `uk_active_per_furnace` (`furnace_code`, `active_once`),
  KEY `idx_disposal_status` (`status`),
  KEY `idx_disposal_furnace` (`furnace_code`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常工况处置单';

-- 处置单与环保告警的多对多证据关联（并发到达的后到告警挂到既有处置单，不丢失）
DROP TABLE IF EXISTS `abnormal_disposal_alarm`;
CREATE TABLE `abnormal_disposal_alarm` (
  `id`            BIGINT      NOT NULL AUTO_INCREMENT,
  `disposal_id`   BIGINT      NOT NULL COMMENT '处置单 id',
  `alarm_id`      BIGINT      NOT NULL COMMENT '环保告警 id',
  `bind_source`   VARCHAR(16) NOT NULL DEFAULT 'SNAPSHOT' COMMENT '证据来源 SNAPSHOT/MERGED',
  `bind_time`     DATETIME    NOT NULL COMMENT '挂接时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_disposal_alarm` (`disposal_id`, `alarm_id`),
  KEY `idx_da_alarm` (`alarm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处置单-告警证据关联';

-- 处置单操作轨迹（每次状态推进、失败的提交与复核拒绝均留痕）
DROP TABLE IF EXISTS `abnormal_disposal_trace`;
CREATE TABLE `abnormal_disposal_trace` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `disposal_id`   BIGINT       NOT NULL COMMENT '处置单 id',
  `disposal_no`   VARCHAR(40)  NOT NULL COMMENT '处置单编号（冗余便于审计）',
  `action`        VARCHAR(32)  NOT NULL COMMENT '动作 CREATE/MEASURE/REQUEST_REVIEW/REVIEW_RECOVER/CLOSE/MERGE_ALARM/DATA_MISSING_CHOICE/REJECT/FAIL',
  `from_status`   VARCHAR(16)  DEFAULT NULL COMMENT '动作前状态',
  `to_status`     VARCHAR(16)  DEFAULT NULL COMMENT '动作后状态',
  `operator_name` VARCHAR(32)  DEFAULT NULL COMMENT '操作人',
  `result`        VARCHAR(16)  NOT NULL DEFAULT 'SUCCESS' COMMENT '结果 SUCCESS/FAILED',
  `detail`        VARCHAR(512) DEFAULT NULL COMMENT '说明或失败原因',
  `operate_time`  DATETIME     NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_disposal` (`disposal_id`, `operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处置单操作轨迹';

-- =========================================================
-- 种子数据（进厂时间用 UTC_TIMESTAMP() + INTERVAL 8 HOUR 相对偏移，保证任何时候演示都是「当日」数据）
-- =========================================================

-- 进厂车次：14 车次，覆盖 4 类垃圾与 5 个行政区划
INSERT INTO `intake_record`
  (`truck_no`, `source_region`, `transfer_station`, `waste_category`, `gross_weight_ton`, `tare_weight_ton`,
   `net_weight_ton`, `intake_time`, `scale_no`, `calorific_value`, `driver_name`, `unload_pit`, `remark`) VALUES
  ('粤Q·A1283', '江城区', '城东中转站', 'KITCHEN',    28.60, 13.20, 15.40, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 280 MINUTE), 'SCALE-01', 4200, '陈志强', 'P-01', '厨余垃圾，含水率偏高'),
  ('粤Q·B2071', '阳东区', '阳东压缩站', 'OTHER',      31.20, 13.80, 17.40, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 260 MINUTE), 'SCALE-02', 7600, '李海生', 'P-01', NULL),
  ('粤Q·C5530', '阳西县', '阳西中转站', 'OTHER',      29.80, 13.50, 16.30, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 240 MINUTE), 'SCALE-01', 7300, '王建国', 'P-02', NULL),
  ('粤Q·D0916', '海陵区', '海陵中转站', 'RECYCLABLE', 26.40, 13.10, 13.30, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 220 MINUTE), 'SCALE-03', 9100, '张伟明', 'P-03', '含大量废纸塑，热值高'),
  ('粤Q·E3328', '高新区', '高新中转站', 'OTHER',      30.10, 13.60, 16.50, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 200 MINUTE), 'SCALE-02', 7800, '刘文彬', 'P-02', NULL),
  ('粤Q·A7120', '江城区', '城南中转站', 'KITCHEN',    27.90, 13.30, 14.60, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 180 MINUTE), 'SCALE-01', 4100, '陈志强', 'P-01', NULL),
  ('粤Q·B4412', '阳东区', '东平中转站', 'OTHER',      32.40, 13.90, 18.50, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 160 MINUTE), 'SCALE-03', 7500, '黄启明', 'P-04', NULL),
  ('粤Q·F1077', '江城区', '城北压缩站', 'HAZARDOUS',  22.10, 13.00,  9.10, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 140 MINUTE), 'SCALE-02', 2800, '周立军', 'P-05', '危废转移联单 WH2026-0912，分拣暂存后入炉'),
  ('粤Q·C8891', '阳西县', '织篢中转站', 'OTHER',      30.70, 13.70, 17.00, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 120 MINUTE), 'SCALE-01', 7600, '王建国', 'P-02', NULL),
  ('粤Q·G2205', '高新区', '港口压缩站', 'RECYCLABLE', 25.80, 13.00, 12.80, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 100 MINUTE), 'SCALE-03', 9300, '吴俊杰', 'P-03', NULL),
  ('粤Q·A3390', '江城区', '城东中转站', 'KITCHEN',    28.10, 13.40, 14.70, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  80 MINUTE), 'SCALE-02', 4300, '陈志强', 'P-01', NULL),
  ('粤Q·B6654', '阳东区', '阳东压缩站', 'OTHER',      31.60, 13.80, 17.80, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  60 MINUTE), 'SCALE-01', 7400, '李海生', 'P-02', NULL),
  ('粤Q·D7742', '海陵区', '海陵中转站', 'KITCHEN',    27.30, 13.20, 14.10, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  40 MINUTE), 'SCALE-03', 4200, '张伟明', 'P-01', NULL),
  ('粤Q·E9918', '高新区', '高新中转站', 'OTHER',      29.40, 13.50, 15.90, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  20 MINUTE), 'SCALE-02', 7700, '刘文彬', 'P-04', NULL);

-- 垃圾储坑：6 个坑位，P-02 满仓、P-03 接近满仓（96%）、P-06 清空检修
INSERT INTO `storage_pit`
  (`pit_no`, `pit_name`, `capacity_ton`, `current_stock_ton`, `fermentation_days`, `leachate_pumped_m3`,
   `pit_temp_c`, `grab_no`, `status`, `remark`) VALUES
  ('P-01', '1号垃圾储坑', 8000.00, 6120.00, 5.5, 386.50, 48.6, 'GRAB-01', 'FERMENTING', '发酵良好，抓斗混料中'),
  ('P-02', '2号垃圾储坑', 8000.00, 8000.00, 7.2, 452.30, 52.4, 'GRAB-02', 'FULL',       '已满仓，停止卸料，优先投料'),
  ('P-03', '3号垃圾储坑', 6000.00, 5760.00, 6.8, 341.20, 51.1, 'GRAB-03', 'FERMENTING', '接近满仓（96%），限制卸料'),
  ('P-04', '4号垃圾储坑', 6000.00, 3240.00, 3.6, 208.70, 43.2, 'GRAB-04', 'FERMENTING', '新料为主，发酵中段'),
  ('P-05', '5号垃圾储坑', 4000.00,  960.00, 2.1,  96.40, 38.5, 'GRAB-01', 'NORMAL',     '备用坑，接收高峰期来料'),
  ('P-06', '6号垃圾储坑', 4000.00,    0.00, 0.0,  12.30, 26.0, NULL,      'OVERHAUL',   '清空检修，防腐层修补中');

-- 焚烧炉：3 台炉排炉 + 2 台流化床 + 2 台回转窑（子类由判别公式 CASE WHEN 得出）
INSERT INTO `incinerator`
  (`furnace_code`, `furnace_name`, `furnace_type_code`, `rated_capacity_tpd`, `furnace_temp_c`, `flue_gas_residence_s`,
   `co_ppm`, `combustion_status`, `daily_process_ton`, `running_hours`, `last_start_time`, `grab_no`, `stack_code`, `remark`,
   `grate_speed_mh`, `grate_bed_thickness_mm`, `grate_stage_count`, `primary_air_temp_c`,
   `bed_material_height_mm`, `fluidizing_air_speed_ms`, `bed_temp_c`, `bed_material_size_mm`,
   `kiln_speed_rpm`, `kiln_inclination_deg`, `kiln_length_m`, `kiln_tail_temp_c`) VALUES
  -- 炉排炉
  ('F-01', '1号机械炉排炉', 'GRATE', 750.00, 912.0, 2.60,  68.00, 'STABLE',   726.50, 6480.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 27 DAY), 'GRAB-01', 'STACK-F01', '工况稳定，负荷 96%',
     6.80, 820.0, 5, 218.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  ('F-02', '2号机械炉排炉', 'GRATE', 750.00, 867.0, 2.10,  95.00, 'STABLE',   692.30, 6120.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 19 DAY), 'GRAB-02', 'STACK-F02', '一次风量偏大，注意料层',
     5.40, 760.0, 5, 205.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  ('F-03', '3号机械炉排炉', 'GRATE', 600.00, 806.0, 1.80, 182.00, 'STARTING',  318.60, 5040.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  6 HOUR), 'GRAB-03', 'STACK-F03', '启炉升温中，未达 850℃ 达标线',
     4.20, 610.0, 4, 186.0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  -- 流化床
  ('F-04', '1号循环流化床', 'FLUIDIZED', 400.00, 884.0, 2.30,  72.00, 'STABLE', 382.40, 4320.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 33 DAY), 'GRAB-02', 'STACK-F04', '床温均匀，返料正常',
     NULL, NULL, NULL, NULL, 1150.0, 3.60, 872.0, 1.20, NULL, NULL, NULL, NULL),
  ('F-05', '2号循环流化床', 'FLUIDIZED', 350.00, 858.0, 2.00,  88.00, 'STABLE', 326.80, 3960.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 24 DAY), 'GRAB-04', 'STACK-F05', '石英砂床料补充过 2 次',
     NULL, NULL, NULL, NULL, 1080.0, 3.25, 846.0, 1.05, NULL, NULL, NULL, NULL),
  -- 回转窑
  ('F-06', '1号回转窑', 'ROTARY', 200.00, 895.0, 2.80,  60.00, 'STABLE',   178.20, 2880.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 41 DAY), 'GRAB-01', 'STACK-F06', '协同处置少量一般工业固废',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1.85, 2.50, 14.60, 968.0),
  ('F-07', '2号回转窑', 'ROTARY', 150.00, 742.0, 1.60, 210.00, 'SHUTDOWN',   92.50, 1920.0, DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 62 DAY), NULL,      'STACK-F07', '停炉待修：窑尾耐火砖更换',
     NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0.60, 2.20, 12.80, 705.0);

-- 启停炉与运行记录：12 条，覆盖 3 个月份与 5 台炉
INSERT INTO `furnace_run_log`
  (`furnace_code`, `action`, `operate_time`, `period_month`, `furnace_temp_c`, `process_ton`, `power_kwh`, `run_hours`, `operator`, `remark`) VALUES
  ('F-01', 'START',     DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 62 DAY), '2026-07', 902.0, 712.40, 227968.00, 24.0, '王强',   '计划启炉，升温曲线正常'),
  ('F-01', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 61 DAY), '2026-07', 918.0, 726.80, 232576.00, 24.0, '王强',   '转入稳燃，负荷 97%'),
  ('F-02', 'START',     DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 45 DAY), '2026-08', 886.0, 668.20, 213824.00, 24.0, '李国栋', '检修后启炉'),
  ('F-02', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 44 DAY), '2026-08', 874.0, 681.50, 218080.00, 24.0, '李国栋', '稳燃运行'),
  ('F-04', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 31 DAY), '2026-08', 878.0, 374.60, 119872.00, 24.0, '赵鹏',   '流化床床温稳定'),
  ('F-04', 'STOP',      DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 29 DAY), '2026-08', 812.0, 142.30,  45536.00, 12.0, '赵鹏',   '计划停炉清焦'),
  ('F-05', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 22 DAY), '2026-09', 856.0, 322.40, 103168.00, 24.0, '孙立',   '月度运行'),
  ('F-06', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 17 DAY), '2026-09', 891.0, 176.50,  56480.00, 24.0, '周涛',   '回转窑窑速稳定'),
  ('F-03', 'START',     DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  9 HOUR), '2026-09', 782.0,  96.20,  30784.00,  8.0, '王强',   '启炉升温，尚未达标'),
  ('F-01', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  8 HOUR), '2026-09', 912.0, 231.40,  74048.00,  8.0, '王强',   '当班稳燃'),
  ('F-02', 'STABILIZE', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  7 HOUR), '2026-09', 867.0, 224.80,  71936.00,  8.0, '李国栋', '当班稳燃'),
  ('F-07', 'STOP',      DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), '2026-09', 742.0,  48.60,  15552.00,  6.0, '周涛',   '窑尾耐火砖破损，紧急停炉');

-- 余热锅炉：4 台
INSERT INTO `boiler`
  (`boiler_no`, `boiler_name`, `furnace_code`, `steam_pressure_mpa`, `steam_temp_c`, `evaporation_tph`,
   `feed_water_temp_c`, `flue_gas_temp_c`, `rated_evaporation_tph`, `status`, `remark`) VALUES
  ('B-01', '1号余热锅炉', 'F-01', 4.10, 400.0, 78.50, 130.0, 195.0, 85.00, 'RUNNING',  '中温次高压参数'),
  ('B-02', '2号余热锅炉', 'F-02', 3.95, 396.0, 72.30, 128.0, 190.0, 85.00, 'RUNNING',  '受热面吹灰已完成'),
  ('B-03', '3号余热锅炉', 'F-04', 3.82, 392.5, 51.60, 126.0, 198.0, 60.00, 'RUNNING',  '配套流化床'),
  ('B-04', '4号余热锅炉', 'F-06', 3.70, 385.0, 26.80, 124.0, 205.0, 32.00, 'BANKING',  '回转窑停炉，热备中');

-- 汽轮发电机组：3 台。generation/grid/auxiliary_rate 为最近统计日（当日增量口径）；
-- main_meter_reading_kwh / grid_meter_reading_kwh 为当前累计表码，日结按相邻表码差值取增量，不再把累计表码当单日发电量
INSERT INTO `turbine_generator`
  (`unit_no`, `unit_name`, `capacity_mw`, `generation_kwh`, `grid_kwh`, `auxiliary_rate`, `running_hours`,
   `main_meter_reading_kwh`, `grid_meter_reading_kwh`, `meter_no`, `basis_source`, `stat_date`, `status`, `remark`) VALUES
  ('TG-01', '1号汽轮发电机组', 25.00, 482000.00, 402470.00, 16.50, 24.0,
    15860000.00, 13240000.00, 'EM-TG01-01', 'PREV_DAY', DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), 'ON_GRID', '凝汽式机组，主电能表累计 1586 万 kWh'),
  ('TG-02', '2号汽轮发电机组', 25.00, 441600.00, 370400.00, 16.12, 24.0,
    14520000.00, 12180000.00, 'EM-TG02-01', 'PREV_DAY', DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), 'ON_GRID', '抽凝式机组，主电能表累计 1452 万 kWh'),
  ('TG-03', '3号汽轮发电机组', 12.00, 201600.00, 168100.00, 16.62, 24.0,
    6980000.00, 5820000.00, 'EM-TG03-01', 'PREV_DAY', DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), 'ON_GRID', '小机组，调峰运行'),
  ('TG-04', '4号汽轮发电机组', 25.00, 468200.00, 391800.00, 16.32, 24.0,
    17620000.00, 14690000.00, 'EM-TG04-01', 'PREV_DAY', DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), 'ON_GRID', '凝汽式机组'),
  ('TG-05', '5号汽轮发电机组', 12.00, 198400.00, 165300.00, 16.68, 24.0,
    8240000.00, 6860000.00, 'EM-TG05-01', 'PREV_DAY', DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), 'ON_GRID', '小机组，调峰运行');

-- 烟气排放指标：6 项 × 3 个时段，其中 5 项在最近时段超标
INSERT INTO `emission_record`
  (`indicator_code`, `indicator_name`, `measured_value`, `limit_value`, `unit`, `exceeded`, `exceed_ratio`,
   `period_label`, `monitor_time`, `stack_code`, `emission_flag`, `remark`) VALUES
  ('PARTICULATE', '颗粒物',        12.600, 20.000, 'mg/m³',     0, -37.00, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', '布袋除尘器压差正常'),
  ('PARTICULATE', '颗粒物',        23.500, 20.000, 'mg/m³',     1,  17.50, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'EXCEED', '布袋破损疑似，已安排检漏'),
  ('PARTICULATE', '颗粒物',        18.900, 20.000, 'mg/m³',     0,  -5.50, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('SO2',         '二氧化硫 SO₂',  42.300, 80.000, 'mg/m³',     0, -47.13, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', '脱酸塔喷浆量充足'),
  ('SO2',         '二氧化硫 SO₂',  88.400, 80.000, 'mg/m³',     1,  10.50, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'EXCEED', '石灰浆制备浓度偏低'),
  ('SO2',         '二氧化硫 SO₂',  65.800, 80.000, 'mg/m³',     0, -17.75, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('NOX',         '氮氧化物 NOx', 186.000, 250.000, 'mg/m³',    0, -25.60, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', 'SNCR 投运正常'),
  ('NOX',         '氮氧化物 NOx', 232.500, 250.000, 'mg/m³',    0,  -7.00, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('NOX',         '氮氧化物 NOx', 268.300, 250.000, 'mg/m³',    1,   7.32, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'EXCEED', '氨水喷射量不足，炉膛温度波动'),
  ('HCL',         '氯化氢 HCl',    22.400, 50.000, 'mg/m³',     0, -55.20, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('HCL',         '氯化氢 HCl',    46.200, 50.000, 'mg/m³',     0,  -7.60, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('HCL',         '氯化氢 HCl',    58.600, 50.000, 'mg/m³',     1,  17.20, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'EXCEED', '来料含氯塑料比例升高'),
  ('CO',          '一氧化碳 CO',   36.500, 80.000, 'mg/m³',     0, -54.38, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('CO',          '一氧化碳 CO',   92.400, 80.000, 'mg/m³',     1,  15.50, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'EXCEED', '燃烧不完全，需调整二次风'),
  ('CO',          '一氧化碳 CO',   71.200, 80.000, 'mg/m³',     0, -11.00, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'NORMAL', '二次风调整后回落'),
  ('DIOXIN',      '二噁英',         0.042,  0.100, 'ng-TEQ/m³', 0, -58.00, '当日 08:00~12:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 10 HOUR), 'STACK-01', 'NORMAL', '季度例行监测'),
  ('DIOXIN',      '二噁英',         0.078,  0.100, 'ng-TEQ/m³', 0, -22.00, '当日 12:00~18:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  5 HOUR), 'STACK-01', 'NORMAL', NULL),
  ('DIOXIN',      '二噁英',         0.131,  0.100, 'ng-TEQ/m³', 1,  31.00, '当日 18:00~24:00 时段', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 'STACK-01', 'EXCEED', '活性炭喷射量需提高');

-- 环保告警：超标自动登记（5 条未处理 + 1 条已处理）
INSERT INTO `env_alarm`
  (`alarm_no`, `indicator_code`, `indicator_name`, `measured_value`, `limit_value`, `exceed_ratio`, `alarm_level`,
   `period_label`, `stack_code`, `alarm_time`, `handled`, `handle_note`) VALUES
  ('ALM20260928001', 'NOX',         '氮氧化物 NOx', 268.300, 250.000,   7.32, 'MAJOR',  '当日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 0, ''),
  ('ALM20260928002', 'HCL',         '氯化氢 HCl',    58.600,  50.000,  17.20, 'MAJOR',  '当日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 0, ''),
  ('ALM20260928003', 'PARTICULATE', '颗粒物',        23.500,  20.000,  17.50, 'MAJOR',  '当日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 0, ''),
  ('ALM20260928004', 'CO',          '一氧化碳 CO',   92.400,  80.000,  15.50, 'MAJOR',  '当日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 0, ''),
  ('ALM20260928005', 'DIOXIN',      '二噁英',         0.131,   0.100,  31.00, 'SEVERE', '当日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL  1 HOUR), 0, ''),
  ('ALM20260927006', 'SO2',         '二氧化硫 SO₂',  88.400,  80.000,  10.50, 'MINOR',  '昨日 18:00~24:00 时段', 'STACK-01', DATE_SUB(UTC_TIMESTAMP() + INTERVAL 8 HOUR, INTERVAL 26 HOUR), 1, '已提高石灰浆制备浓度并复核表码');

-- 灰渣与渗滤液台账：8 天，其中一天出水 COD 超标
INSERT INTO `ash_residue`
  (`record_date`, `fly_ash_ton`, `bottom_ash_ton`, `solidified_ton`, `disposal_dest`, `leachate_treated_m3`,
   `effluent_cod`, `effluent_ok`, `shift`, `remark`) VALUES
  (DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR),                          68.50, 512.30, 62.00, '填埋场专区填埋', 386.50,  78.40, 1, '白班', '炉渣热灼减率 3.1%'),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 1 DAY), 71.20, 528.60, 65.00, '水泥窑协同处置', 392.10,  82.10, 1, '白班', NULL),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 2 DAY), 65.80, 496.40, 60.00, '填埋场专区填埋', 371.80,  74.60, 1, '白班', NULL),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 3 DAY), 73.40, 541.20, 68.00, '水泥窑协同处置', 405.30,  88.90, 1, '白班', NULL),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 4 DAY), 69.10, 519.70, 63.00, '危废刚性填埋场', 388.20,  96.50, 1, '白班', '飞灰螯合固化后送刚性填埋'),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 5 DAY), 74.60, 552.10, 70.00, '综合利用制砖',   412.60, 102.30, 0, '白班', '出水 COD 超标，MBR 膜池清洗中'),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 6 DAY), 66.30, 503.80, 61.00, '填埋场专区填埋', 376.90,  80.20, 1, '白班', NULL),
  (DATE_SUB(DATE(UTC_TIMESTAMP() + INTERVAL 8 HOUR), INTERVAL 7 DAY), 70.90, 526.40, 64.00, '水泥窑协同处置', 395.40,  85.70, 1, '白班', NULL);
