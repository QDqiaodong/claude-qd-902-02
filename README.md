# 生活垃圾焚烧发电厂 · 进厂与焚烧运行管理

以「垃圾进厂计重 → 储坑发酵 → 焚烧炉 → 余热锅炉发电 → 烟气净化 → 灰渣与渗滤液」为主线的焚烧发电厂运行管理系统。

## 技术栈

- 后端：Spring Boot 3.3.0 + Java 17 + Spring Data JPA(Hibernate 6.5) + MySQL 8 + Redis
- 前端：Vue 3 + Element Plus + Vite
- 端口：前端 8292、后端 8392、MySQL 3592、Redis 6592

## 启动

```bash
./start.sh            # 已构建过：直接起
./start.sh --build    # 首次或改过代码：构建并起
./stop.sh             # 暂停
./clean.sh            # 收摊（down -v）
```

访问 http://127.0.0.1:8292/ ，健康检查 http://127.0.0.1:8392/actuator/health

验收脚本：`python3 acceptance/disposal_acceptance.py`（异常工况闭环）、
`python3 acceptance/power_settlement_acceptance.py`（发电日结：跨日/回拨/换表/幂等并发/重新打开页面）

## 五大模块

| 模块 | 说明 | 主要接口 |
| --- | --- | --- |
| 进厂与计量 | 行政区划/中转站来源、车牌、垃圾类别、毛重/皮重/净重、地磅号 | `/api/intake/**` |
| 垃圾储坑 | 坑位容量与库存、堆酵天数、渗滤液抽出、坑温、抓斗投料 | `/api/pits/**` |
| 焚烧炉与炉型 | 炉排炉 / 流化床 / 回转窑 单表继承，炉膛温度、停留时间、启停炉 | `/api/incinerators/**` |
| 余热锅炉与发电 | 蒸汽压力/温度/蒸发量、装机、发电日结（相邻表码增量）、上网电量、厂用电率、表计回拨/更换处置 | `/api/power/**` |
| 环保排放与灰渣 | 六项污染物、超标告警、灰渣与渗滤液台账 | `/api/environment/**` |
| 异常工况处置单 | 炉况判定/同炉严重告警 → 快照冻结投料 → 措施复核 → 恢复闭环 | `/api/disposals/**` |

## 数据访问设计（本仓技术标识）

`Incinerator` 为抽象父类，`GrateFurnace` / `FluidizedBed` / `RotaryKiln` 三个子类共用一张 `incinerator` 表，
由 Hibernate `@DiscriminatorFormula`（SQL CASE WHEN 表达式）判别子类，而非 `@DiscriminatorColumn` 字符串列：

```java
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("CASE WHEN furnace_type_code = 'GRATE' THEN 'GRATE' ... END")
public abstract class Incinerator { ... }
```

配套查询：

- `TYPE()`：`/api/incinerators/stats/type` 按子类统计台数、当日入炉量、平均炉膛温度
- `treat()`：`/api/incinerators/fast-grate?speed=6` 访问子类专属字段（炉排速度）
- 手写 JPQL：按来源区域汇总进厂量与热值、按炉号统计月度处理量与发电量、排放超标次数排行

## 业务规则（service 内校验，抛 BizException）

1. 净重 = 毛重 − 皮重，必须为正；超出地磅量程上限（45 t）拒绝
2. 炉膛温度 < 850℃ 或烟气停留 < 2 s，不允许判定为达标燃烧（逐项列出不达标项）
3. 任一排放指标超标：自动登记环保告警，且不允许在该时段标记为「正常排放」
4. 储坑库存超过容量不允许继续卸料（提示剩余可容纳余量）
5. 发电日结登记的是主电能表**当日抄见累计表码**，系统按「机组 + 统计日 + 相邻表码」
   计算当日发电增量（期末表码 − 期初基数），再据此计算当日上网电量与厂用电率；
   累计表码不得被当成单日发电量。当日增量不得超过 装机容量 × 1000 × 当日运行小时 的理论上限。
   首次日结须提供期初建档基数；之后逐日连续，跨日跳日结、上一日暂停/校表未补结一律拒绝（表码链不能断）。
   表计回拨或更换必须二选一并留痕：`SEAL_NEW_BASE` 旧表在原有效表码封账、新表从声明基数起算
   （后续增量锚定新基数，机组当前表号/表码同步切换）；`SUSPEND` 暂停日结等待校表——当日不出增量、
   该机组后续日期全部挂起，校表确认基数（`VERIFIED`）后对该日补日结才解锁。
   同机组日结由机组行锁串行化，`uk_unit_stat_date` 唯一索引库级兜底；
   请求幂等指纹保证重复请求不产生第二份日结（同口径直接返回原日结），
   较早页面发出的不同口径请求不得覆盖较新日结结果（`REJECT_DUPLICATE` 留痕）；
   校表用 `@Version` 乐观锁拦截过期页面。列表展示当前表码、当日增量、基数来源与异常状态，
   每天的表码、事件与计算依据都落在 `power_daily_settlement`，操作轨迹落在 `power_meter_trace`。

### 异常工况处置单闭环规则

6. 炉温 < 850℃ 或停留 < 2s（燃烧判定/稳燃失败）、或同炉出现 SEVERE 严重排放告警时：
   自动/人工开立处置单，保存炉况、严重告警证据、炉侧+坑位供料抓斗快照，并**冻结该炉后续投料**；
   全程只新增处置数据，不改动任何已完成运行记录。
7. 同一炉未关闭前只允许一张有效处置单（`active_once` 生成列 + 唯一索引库级兜底）。
   自动触发与人工发起并发到达时合并证据：不重复冻结，后到严重告警挂 `MERGED` 证据；
   待复核/已恢复阶段新到严重告警会把单据反弹回处置中并重新冻结。
8. 状态机：待处置 PENDING → 处置中 HANDLING →（负责人记录措施并申请复核）→ 待复核 REVIEWING
   →（**另一名复核人员**确认燃烧重新达标且严重告警解除）→ 已恢复 RECOVERED（解冻）→ 已关闭 CLOSED。
   `@Version` 乐观锁拒绝重复点击与过期版本；失败原因写入操作轨迹。
9. 告警数据暂不可用时必须二选一并展示恢复条件影响：
   `WAIT_FREEZE` 继续冻结等待补齐（补齐后仍须达标+告警解除才能复核恢复）；
   `CLOSE_KEEP_FROZEN` 记录理由后关闭处置但投料保持冻结、不可再复核恢复（须重新开单走完整闭环）。
10. 列表/详情/炉况入口/储坑投料共用同一联锁口径（`frozen-furnaces`），刷新与多人操作后状态一致；
    储坑投料目标炉被冻结时前端置灰、后端行锁拒绝。

## 目录

```
backend/src/main/java/com/waste/power/
  common/      统一返回与异常处理、垃圾发电词典 Dict
  domain/      11 个实体（含 Incinerator 继承体系、发电日结与表计轨迹）
  dto/         请求/视图与 JPQL 投影对象
  repository/  数据访问（含 TYPE() / treat() / 手写 JPQL）
  service/     业务规则
  controller/  /api 前缀接口
mysql/schema.sql            建表 + 种子数据（构建期导入）
backend/src/main/resources/schema.sql  同上副本
frontend/src/               App.vue（顶部工艺流程导航）+ 四个页面
```
