#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
汽轮发电机组发电日结 · 验收脚本（仅用标准库，对运行中的系统 http://127.0.0.1:8392 打接口）

修复点：日结提交的是主电能表【当日累计表码】，系统按机组 + 统计日 + 相邻表码计算当日发电增量，
       再算上网电量与厂用电率；累计表码不得被当成单日发电量。

覆盖场景：
  A. 首次建档基数 → 连续两日日结：当日增量 = 相邻表码差值，上网电量/厂用电率按增量计算
  B. 跨日跳日结被拒（表码链不能断）
  C. 重复请求幂等：同口径重复提交不产生第二份日结；不同口径（较早页面）不得改掉较新结果
  D. 表计回拨：未做处置选择被拒；旧表封账启用新基数，后续日期沿新基数口径
  E. 表计回拨：暂停日结等待校表 → 当日不出增量、后续日期被拦 → 校表确认 → 补日结解锁
  F. 两人同时操作（同口径并发 / 不同口径并发）：只有一份日结
  G. 重新打开页面：列表/机组视图与最新日结一致；过期版本校表被拒
  H. 留痕：SETTLE / IDEMPOTENT_RETRY / REJECT_DUPLICATE / SUSPEND / VERIFY / SEAL_NEW_BASE 轨迹齐全

用法：
  ./start.sh --build && python3 acceptance/power_settlement_acceptance.py
"""
import datetime
import json
import sys
import threading
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8392/api"
PASS, FAIL = 0, 0


def call(method, path, body=None, expect_ok=None, expect_fail_contains=None, expect_fail_any=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            payload = json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        payload = json.loads(e.read().decode())
    ok = payload.get("ok")
    if expect_ok is True and ok is not True:
        raise AssertionError(f"{method} {path} 应成功，实际失败：{payload.get('message')}")
    if expect_fail_contains:
        if ok is not False:
            raise AssertionError(f"{method} {path} 应被拒绝，实际成功：{payload}")
        msg = payload.get("message") or ""
        for part in expect_fail_contains:
            if part not in msg:
                raise AssertionError(f"{method} {path} 报错应包含 {part!r}，实际：{msg}")
    if expect_fail_any:
        if ok is not False:
            raise AssertionError(f"{method} {path} 应被拒绝，实际成功：{payload}")
        msg = payload.get("message") or ""
        if not any(part in msg for part in expect_fail_any):
            raise AssertionError(
                f"{method} {path} 报错应包含任一 {expect_fail_any!r}，实际：{msg}")
    return payload


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ✅ {name}")
    else:
        FAIL += 1
        print(f"  ❌ {name} {detail}")


def data(payload):
    return payload["data"]


def settle(unit, body, **kw):
    return call("POST", f"/power/turbines/{unit}/daily-settlement", body, **kw)


def get_settlement(unit, date):
    items = data(call("GET", f"/power/daily-settlements?unitNo={unit}"))
    hit = [s for s in items if s["statDate"] == date]
    return hit[0] if hit else None


def traces(sid):
    return data(call("GET", f"/power/daily-settlements/{sid}/traces"))


def turbines_map():
    return {t["unitNo"]: t for t in data(call("GET", "/power/turbines"))}


def scenario_a_basic(d1, d2, d3):
    print("\n[A] 首次建档基数 + 连续两日日结（相邻表码取增量；累计表码≠单日发电量）")
    # 首次日结：期初建档基数 10,000,000，期末 10,450,000 → 当日增量 450,000
    r = settle("TG-01", {
        "statDate": d1, "initialBaseReadingKwh": 10000000,
        "currentReadingKwh": 10450000, "runningHours": 24,
        "gridKwh": 378000, "operator": "甲值-张三"
    }, expect_ok=True)
    s = data(r)
    check("首日增量=期末−期初=450,000", s["generationKwh"] == 450000.00, str(s["generationKwh"]))
    check("首日基数来源为首次建档", s["basisSource"] == "INIT_BASE", s["basisSource"])
    check("首日上网电量取填报值", s["gridKwh"] == 378000.00)
    check("首日厂用电率=(450000−378000)/450000=16%", s["auxiliaryRate"] == 16.00, str(s["auxiliaryRate"]))
    check("日结状态为已日结", s["status"] == "SETTLED", s["status"])
    check("无异常", s["anomalyStatus"] == "NORMAL", s["anomalyStatus"])

    # 次日：只提交新累计表码，期初自动取上日期末（相邻表码）
    r2 = settle("TG-01", {
        "statDate": d2, "currentReadingKwh": 10920000,
        "runningHours": 24, "operator": "乙值-李四"
    }, expect_ok=True)
    s2 = data(r2)
    check("次日增量=10,920,000−10,450,000=470,000（不是 10,920,000）",
          s2["generationKwh"] == 470000.00, str(s2["generationKwh"]))
    check("次日基数来源为上日日结表码", s2["basisSource"] == "PREV_DAY", s2["basisSource"])
    check("次日期初表码=首日期末表码", s2["prevReadingKwh"] == 10450000.00)
    check("次日上网电量缺省按85%估算=399,500", s2["gridKwh"] == 399500.00, str(s2["gridKwh"]))
    check("次日厂用电率15%", s2["auxiliaryRate"] == 15.00, str(s2["auxiliaryRate"]))
    check("日结记录保留每天计算依据（期末表码 10,920,000）", s2["currentReadingKwh"] == 10920000.00)

    # 机组视图：展示当前表码 + 当日增量（最近统计日）
    t = turbines_map()["TG-01"]
    check("机组视图当前表码=最新累计表码", t["mainMeterReadingKwh"] == 10920000.00, str(t["mainMeterReadingKwh"]))
    check("机组视图发电量为当日增量而非累计", t["generationKwh"] == 470000.00, str(t["generationKwh"]))
    check("机组视图基数来源=上日日结", t["basisSource"] == "PREV_DAY")
    check("机组视图无异常状态", t["anomalyStatus"] == "NORMAL", str(t["anomalyStatus"]))
    return s, s2


def scenario_b_cross_day_gap(d1, d3):
    print("\n[B] 跨日跳日结被拒（必须逐日补齐，表码链不能断）")
    # 全新机组 TG-02，只结了 d1，直接结 d3
    settle("TG-02", {
        "statDate": d1, "initialBaseReadingKwh": 8000000,
        "currentReadingKwh": 8420000, "runningHours": 24
    }, expect_ok=True)
    call("POST", "/power/turbines/TG-02/daily-settlement", {
        "statDate": d3, "currentReadingKwh": 9300000
    }, expect_fail_contains=["补齐", "跨日"])
    check("跳日结已被拒绝且留痕",
          any("跨日" in (x["detail"] or "") for x in _latest_traces("TG-02")))
    # 未来日期也拒绝
    future = (datetime.date.fromisoformat(d3) + datetime.timedelta(days=1)).isoformat()
    call("POST", "/power/turbines/TG-02/daily-settlement", {
        "statDate": future, "currentReadingKwh": 9300000
    }, expect_fail_any=["不能提前日结", "晚于当天"])


def _latest_traces(unit):
    # 机组级轨迹（含被拒日结尚无单 id 的失败留痕，如跨日跳日结）
    return data(call("GET", f"/power/turbines/{unit}/meter-traces"))


def scenario_c_idempotent(d1):
    print("\n[C] 重复请求幂等 + 较早页面不同请求不得覆盖较新日结")
    # TG-03 装机 12 MW，当日理论上限 288,000 kWh，读数按小机组量级
    payload = {"statDate": d1, "initialBaseReadingKwh": 6000000,
               "currentReadingKwh": 6200000, "runningHours": 24}
    first = data(settle("TG-03", payload, expect_ok=True))
    # 完全相同请求再来一次（操作人不同不影响指纹）
    again = data(settle("TG-03", {**payload, "operator": "另一页面-王五"}, expect_ok=True))
    check("重复请求返回同一条日结（无第二份）", again["id"] == first["id"])
    items = data(call("GET", "/power/daily-settlements?unitNo=TG-03"))
    same_day = [x for x in items if x["statDate"] == d1]
    check("库中当日只有一份日结", len(same_day) == 1, f"{len(same_day)} 份")
    check("增量未被重复请求改变", same_day[0]["generationKwh"] == 200000.00)
    tr = traces(first["id"])
    check("留痕包含幂等返回 IDEMPOTENT_RETRY", any(t["action"] == "IDEMPOTENT_RETRY" for t in tr),
          json.dumps([t["action"] for t in tr], ensure_ascii=False))

    # 较早页面用不同表码重发同日 → 拒绝且留痕，较新结果不变
    call("POST", "/power/turbines/TG-03/daily-settlement", {
        "statDate": d1, "initialBaseReadingKwh": 6000000,
        "currentReadingKwh": 6280000, "runningHours": 24
    }, expect_fail_any=["日结已完成", "过期页面"])
    after = get_settlement("TG-03", d1)
    check("较早页面不同请求未改掉较新日结", after["currentReadingKwh"] == 6200000.00
          and after["generationKwh"] == 200000.00)
    tr2 = traces(first["id"])
    check("覆盖尝试留痕 REJECT_DUPLICATE(FAILED)",
          any(t["action"] == "REJECT_DUPLICATE" and t["result"] == "FAILED" for t in tr2))
    return first


def scenario_d_rollback_seal(d1, d2):
    print("\n[D] 表计回拨：必须二选一；旧表封账并启用新基数，后续日期沿用新口径")
    # 用全新机组 TG-02 做回拨：d1 已在 B 场景结过（基数 8,000,000 → 8,420,000）
    # 次日表码反而变小（回拨到 500,000），不传处置选择 → 拒绝
    call("POST", "/power/turbines/TG-02/daily-settlement", {
        "statDate": d2, "currentReadingKwh": 500000
    }, expect_fail_contains=["回拨", "SEAL_NEW_BASE", "SUSPEND"])
    # 选择封账：旧表在 8,420,000 封账，原表重新定底基数 0，当日期末 480,000 → 增量 480,000
    r = settle("TG-02", {
        "statDate": d2, "currentReadingKwh": 480000,
        "meterEvent": "ROLLBACK", "decision": "SEAL_NEW_BASE",
        "newMeterBaseReadingKwh": 0, "decisionReason": "现场检查表计被误回拨，旧读数封账并按零底重新计量",
        "operator": "仪表班-赵六"
    }, expect_ok=True)
    s = data(r)
    check("回拨封账当日增量=480,000−0=480,000", s["generationKwh"] == 480000.00, str(s["generationKwh"]))
    check("旧表封账表码=8,420,000", s["oldMeterSealedReadingKwh"] == 8420000.00, str(s["oldMeterSealedReadingKwh"]))
    check("新基数来源=换表/定底新基数", s["basisSource"] == "NEW_METER_BASE", s["basisSource"])
    check("异常状态=表计回拨", s["anomalyStatus"] == "METER_ROLLBACK", s["anomalyStatus"])
    check("处置选择已留痕", s["decision"] == "SEAL_NEW_BASE" and s["decisionReason"])
    tr = traces(s["id"])
    check("留痕 SEAL_NEW_BASE", any(t["action"] == "SEAL_NEW_BASE" for t in tr))

    # 后续日期沿新基数口径：d3 期末 950,000 → 增量 950,000−480,000=470,000
    r2 = settle("TG-02", {
        "statDate": (datetime.date.fromisoformat(d2) + datetime.timedelta(days=1)).isoformat(),
        "currentReadingKwh": 950000, "runningHours": 24
    }, expect_ok=True)
    s2 = data(r2)
    check("次日沿新口径：期初=480,000，增量=470,000",
          s2["prevReadingKwh"] == 480000.00 and s2["generationKwh"] == 470000.00,
          f"{s2['prevReadingKwh']}/{s2['generationKwh']}")
    check("次日基数来源恢复为上日日结表码", s2["basisSource"] == "PREV_DAY", s2["basisSource"])


def scenario_e_suspend_verify(d1, d2, d3):
    print("\n[E] 表计回拨选择暂停日结等待校表 → 后续被拦 → 校表确认 → 补日结解锁（TG-03 继续）")
    # TG-03 在 C 场景 d1 已结（6,000,000 → 6,200,000）。d2 表码回落到 5,900,000
    r = settle("TG-03", {
        "statDate": d2, "currentReadingKwh": 5900000,
        "meterEvent": "ROLLBACK", "decision": "SUSPEND",
        "decisionReason": "表码异常下降，暂停日结并通知计量班校表", "operator": "仪表班-钱七"
    }, expect_ok=True)
    s = data(r)
    check("暂停记录状态=暂停待校表", s["status"] == "SUSPENDED", s["status"])
    check("暂停当日不出增量", s["generationKwh"] is None, str(s["generationKwh"]))
    check("暂停记录保留当日期末表码", s["currentReadingKwh"] == 5900000.00)
    check("暂停记录异常状态", s["anomalyStatus"] == "SUSPENDED")
    tr = traces(s["id"])
    check("留痕 SUSPEND", any(t["action"] == "SUSPEND" for t in tr))
    sid, ver = s["id"], s["version"]

    # 暂停期间后续日期日结被拦
    call("POST", "/power/turbines/TG-03/daily-settlement", {
        "statDate": d3, "currentReadingKwh": 6400000
    }, expect_fail_any=["前一统计日", "校表"])
    # 同日不同请求也不能绕过暂停
    call("POST", "/power/turbines/TG-03/daily-settlement", {
        "statDate": d2, "currentReadingKwh": 6250000
    }, expect_fail_any=["暂停日结", "校表"])
    # 相同请求幂等返回，不产生第二份
    same = data(settle("TG-03", {
        "statDate": d2, "currentReadingKwh": 5900000,
        "meterEvent": "ROLLBACK", "decision": "SUSPEND",
        "decisionReason": "表码异常下降，暂停日结并通知计量班校表", "operator": "仪表班-钱七"
    }, expect_ok=True))
    check("暂停记录相同请求幂等返回同一条", same["id"] == sid and same["status"] == "SUSPENDED")

    # 过期版本校表被拒（重新打开页面场景的版本保护）
    call("POST", f"/power/daily-settlements/{sid}/verify", {
        "version": ver + 99, "confirmedReadingKwh": 6160000, "operator": "计量班-孙八"
    }, expect_fail_any=["版本", "刷新"])

    # 正常校表：确认有效基数 6,160,000
    v = call("POST", f"/power/daily-settlements/{sid}/verify", {
        "version": ver, "confirmedReadingKwh": 6160000,
        "verifyNote": "现场校表：计量装置经校验合格，确认有效基数 616 万", "operator": "计量班-孙八"
    }, expect_ok=True)
    sv = data(v)
    check("校表后状态=已校表待日结", sv["status"] == "VERIFIED", sv["status"])
    check("校表确认基数已记录", sv["verifiedReadingKwh"] == 6160000.00)
    check("校表留痕 VERIFY", any(t["action"] == "VERIFY" for t in traces(sid)))

    # 补日结：按校表确认基数计算增量
    r2 = settle("TG-03", {
        "statDate": d2, "currentReadingKwh": 6250000,
        "runningHours": 24, "operator": "甲值-张三"
    }, expect_ok=True)
    s2 = data(r2)
    check("补日结仍是同一条记录（无第二份）", s2["id"] == sid)
    check("补日结增量=期末6,250,000−校表基数6,160,000=90,000",
          s2["generationKwh"] == 90000.00, str(s2["generationKwh"]))
    check("补日结基数来源=校表确认", s2["basisSource"] == "VERIFIED_BASE", s2["basisSource"])
    check("补日结后状态为已日结", s2["status"] == "SETTLED", s2["status"])

    # 后续日期解锁：d3 正常日结，期初取 d2 期末
    r3 = settle("TG-03", {
        "statDate": d3, "currentReadingKwh": 6420000,
        "runningHours": 24
    }, expect_ok=True)
    s3 = data(r3)
    check("校表补日结后后续日期解锁，增量=170,000",
          s3["generationKwh"] == 170000.00 and s3["prevReadingKwh"] == 6250000.00,
          f"{s3['generationKwh']}")


def scenario_f_concurrent(d2, d3):
    print("\n[F] 两人/两页面同时操作（TG-01 连续日期，并发提交）")
    # 同口径并发：两个线程同时提交完全相同的 d3 日结（行锁串行 + 幂等指纹）
    payload = {"statDate": d3, "currentReadingKwh": 11400000, "runningHours": 24}
    outcomes = {}

    def fire(name):
        outcomes[name] = settle("TG-01", {**payload, "operator": name})

    threads = [threading.Thread(target=fire, args=(n,)) for n in ("并发甲", "并发乙")]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    oks = [o.get("ok") for o in outcomes.values()]
    check("同口径并发两个请求都返回成功（幂等）", oks.count(True) == 2, str(oks))
    ids = {data(o)["id"] for o in outcomes.values() if o.get("ok")}
    check("同口径并发只产生一份日结", len(ids) == 1, str(ids))
    row = get_settlement("TG-01", d3)
    check("并发日结增量正确=480,000", row["generationKwh"] == 480000.00, str(row["generationKwh"]))

    # 不同口径并发：在新机组 TG-02 的「次日已结」基础上，构造一个尚未存在的日期需要两台机组，
    # 这里改用串行模拟「几乎同时的不同内容」：同一已完成日用两个不同读数提交，后者必须被拒。
    call("POST", "/power/turbines/TG-01/daily-settlement", {
        "statDate": d3, "currentReadingKwh": 11555555, "runningHours": 24
    }, expect_fail_any=["日结已完成", "过期页面"])
    check("不同口径覆盖未生效", get_settlement("TG-01", d3)["currentReadingKwh"] == 11400000.00)
    check("并发/覆盖拒绝均有留痕",
          any(t["action"] == "IDEMPOTENT_RETRY" for t in traces(row["id"]))
          and any(t["action"] == "REJECT_DUPLICATE" for t in traces(row["id"])))


def scenario_h_replace_meter(d1, d2, d3):
    print("\n[H] 更换表计：旧表封账 + 新表启用新基数；暂停态校表后未补结前后续日期仍被拦")
    # TG-04 装机 25 MW：d1 先结一天，d2 更换新表
    settle("TG-04", {
        "statDate": d1, "initialBaseReadingKwh": 17000000,
        "currentReadingKwh": 17460000, "runningHours": 24
    }, expect_ok=True)
    # 换表封账不填新表表号必须被拒
    call("POST", "/power/turbines/TG-04/daily-settlement", {
        "statDate": d2, "currentReadingKwh": 120000,
        "meterEvent": "METER_REPLACED", "decision": "SEAL_NEW_BASE",
        "newMeterBaseReadingKwh": 0, "decisionReason": "旧表损坏更换"
    }, expect_fail_contains=["新表表号"])
    # 正式封账换表：旧表在 17,460,000 封账，新表从 0 起，当日期末 360,000 → 当日增量 360,000
    r = settle("TG-04", {
        "statDate": d2, "currentReadingKwh": 360000, "mainMeterNo": "EM-TG04-02",
        "meterEvent": "METER_REPLACED", "decision": "SEAL_NEW_BASE",
        "newMeterNo": "EM-TG04-02", "newMeterBaseReadingKwh": 0,
        "decisionReason": "旧电能表计量超差报废，更换新表并从 0 起算", "operator": "仪表班-赵六"
    }, expect_ok=True)
    s = data(r)
    check("换表当日增量按新基数=360,000", s["generationKwh"] == 360000.00, str(s["generationKwh"]))
    check("旧表封账表码=17,460,000", s["oldMeterSealedReadingKwh"] == 17460000.00)
    check("登记新表表号", s["newMeterNo"] == "EM-TG04-02" and s["mainMeterNo"] == "EM-TG04-02")
    check("异常状态=更换表计", s["anomalyStatus"] == "METER_REPLACED", s["anomalyStatus"])
    check("留痕 SEAL_NEW_BASE（换表）",
          any(t["action"] == "SEAL_NEW_BASE" for t in traces(s["id"])))
    t = turbines_map()["TG-04"]
    check("机组当前表号已切换为新表", t["meterNo"] == "EM-TG04-02", t["meterNo"])
    check("机组当前表码=新表读数口径", t["mainMeterReadingKwh"] == 360000.00)

    # 后续日期沿新表口径
    r2 = settle("TG-04", {
        "statDate": d3, "currentReadingKwh": 820000, "runningHours": 24
    }, expect_ok=True)
    s2 = data(r2)
    check("换表次日期初=新表 360,000、增量=460,000",
          s2["prevReadingKwh"] == 360000.00 and s2["generationKwh"] == 460000.00,
          f"{s2['prevReadingKwh']}/{s2['generationKwh']}")

    # VERIFIED 但尚未补日结前，后续日期不能结（口径未定）—— 用全新小机组 TG-05
    settle("TG-05", {
        "statDate": d1, "initialBaseReadingKwh": 8000000,
        "currentReadingKwh": 8200000, "runningHours": 24
    }, expect_ok=True)
    su = data(settle("TG-05", {
        "statDate": d2, "currentReadingKwh": 8100000,
        "meterEvent": "ROLLBACK", "decision": "SUSPEND",
        "decisionReason": "验收：校表后先不补结，验证后续日期仍被拦", "operator": "仪表班-钱七"
    }, expect_ok=True))
    call("POST", f"/power/daily-settlements/{su['id']}/verify", {
        "version": su["version"], "confirmedReadingKwh": 8180000,
        "verifyNote": "验收校表", "operator": "计量班-孙八"
    }, expect_ok=True)
    call("POST", "/power/turbines/TG-05/daily-settlement", {
        "statDate": d3, "currentReadingKwh": 8420000
    }, expect_fail_any=["尚未完成补日结", "校表"])
    check("已校表未补结时后续日期日结被拦", True)


def scenario_g_reopen_page():
    print("\n[G] 重新打开页面：列表、机组视图、轨迹彼此一致")
    units = turbines_map()
    items = data(call("GET", "/power/daily-settlements"))
    check("日结列表可查询且每天一条（按机组+统计日唯一）", len(items) >= 6, f"{len(items)} 条")
    # 列表字段齐全：当前表码、当日增量、基数来源、异常状态
    required = ["currentReadingKwh", "generationKwh", "basisSource", "anomalyStatus",
                "prevReadingKwh", "gridKwh", "auxiliaryRate"]
    check("列表每行含当前表码/当日增量/基数来源/异常状态",
          all(all(k in row for k in required) for row in items))
    # TG-03 最新视图与最新日结一致
    t = units["TG-03"]
    tg03 = [x for x in items if x["unitNo"] == "TG-03"]
    latest = max(tg03, key=lambda x: x["statDate"])
    check("机组视图与最新日结：当前表码一致",
          t["mainMeterReadingKwh"] == latest["currentReadingKwh"],
          f"{t['mainMeterReadingKwh']} vs {latest['currentReadingKwh']}")
    check("机组视图与最新日结：当日增量一致",
          t["generationKwh"] == latest["generationKwh"])
    check("机组视图与最新日结：基数来源一致",
          t["basisSource"] == latest["basisSource"])
    check("机组视图与最新日结：异常状态一致",
          t["anomalyStatus"] == latest["anomalyStatus"])
    # 每条日结都可查到依据轨迹
    no_trace = [x["id"] for x in tg03 if not traces(x["id"])]
    check("每天的日结都有操作轨迹（计算依据留痕）", not no_trace, str(no_trace))


def main():
    print("=" * 72)
    print("汽轮发电机组发电日结验收（相邻表码增量口径 / 回拨换表 / 幂等与并发）")
    print("=" * 72)
    try:
        health = urllib.request.urlopen(BASE.replace("/api", "") + "/actuator/health", timeout=5)
        json.loads(health.read().decode())
    except Exception as e:
        print(f"系统未运行或不可达：{e}\n请先执行 ./start.sh --build")
        sys.exit(2)

    today = datetime.date.today()
    d1 = (today - datetime.timedelta(days=2)).isoformat()
    d2 = (today - datetime.timedelta(days=1)).isoformat()
    d3 = today.isoformat()
    print(f"验收统计日：{d1} / {d2} / {d3}（以服务器当天为准）")

    scenario_a_basic(d1, d2, d3)
    scenario_c_idempotent(d1)          # TG-03 d1（先建后续回拨场景需要的基线）
    scenario_b_cross_day_gap(d1, d3)   # TG-02 d1
    scenario_d_rollback_seal(d1, d2)   # TG-02 d2 回拨封账、d3 沿新口径
    scenario_e_suspend_verify(d1, d2, d3)  # TG-03 d2 暂停→校表→补日结、d3 解锁
    scenario_f_concurrent(d2, d3)      # TG-01 d3 并发
    scenario_h_replace_meter(d1, d2, d3)   # TG-04 换表封账、TG-05 已校表未补结拦截
    scenario_g_reopen_page()

    print("\n" + "=" * 72)
    print(f"验收结果：通过 {PASS} 项，失败 {FAIL} 项")
    print("=" * 72)
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
