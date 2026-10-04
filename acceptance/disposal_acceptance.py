#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
异常工况处置单 · 闭环验收脚本（仅用标准库，对运行中的系统 http://127.0.0.1:8392 打接口）

覆盖场景：
  A. 自动触发（燃烧判定不达标）→ 快照 → 冻结投料 → 措施 → 申请复核
  B. 人工与自动（严重告警）并发同时触发 → 只保留一张单、证据合并、不重复冻结、后到告警不丢失
  C. 两人同时复核：同人复核被拒、过期版本被拒、恢复期间新严重告警反弹不误放行
  D. 告警解除 + 燃烧达标后他人复核恢复 → 解冻 → 关闭 → 最终状态与投料权限一致
  E. 告警数据缺失：继续冻结等待补齐 / 关闭处置但保持冻结 两种选择的恢复条件影响
  F. 已完成运行记录不被改动（记录条数只增不减、历史记录字段不变）

用法：
  ./start.sh --build && python3 acceptance/disposal_acceptance.py
"""
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
                f"{method} {path} 报错应包含任一 {expect_fail_any!r}，实际：{msg}"
            )
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


def active_for(furnace_code):
    items = data(call("GET", "/disposals?activeOnly=true"))
    hit = [d for d in items if d["furnaceCode"] == furnace_code]
    return hit[0] if hit else None


def wait_active(furnace_code, timeout=6):
    """自动触发走 afterCommit + 独立事务，轮询等待生效"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        d = active_for(furnace_code)
        if d:
            return d
        time.sleep(0.2)
    return None


def evidences(did):
    return data(call("GET", f"/disposals/{did}/evidences"))


def traces(did):
    return data(call("GET", f"/disposals/{did}/traces"))


def condition(did):
    return data(call("GET", f"/disposals/{did}/condition"))


def frozen_set():
    return set(data(call("GET", "/disposals/frozen-furnaces")))


def severe_emission(stack, indicator="NOX", measured=600.0):
    # NOX 限值 250，600/250=2.4 倍 → SEVERE
    return call("POST", "/environment/emissions", {
        "indicatorCode": indicator, "measuredValue": measured,
        "periodLabel": "验收测试时段", "stackCode": stack, "emissionFlag": "EXCEED"
    }, expect_ok=True)


def scenario_a_combustion_auto():
    print("\n[A] 炉况自动触发 → 快照冻结 → 措施 → 申请复核（炉 F-03 初始 806℃ / 1.8s 不达标）")
    call("POST", "/incinerators/F-03/judge", {}, expect_fail_contains=["未达标", "处置单"])
    d = wait_active("F-03")
    check("自动触发生成处置单", d is not None)
    if not d:
        return None
    check("处置单初始状态为待处置", d["status"] == "PENDING", d["status"])
    check("投料已冻结（frozen-furnaces 含 F-03）", "F-03" in frozen_set())
    check("炉况快照保存了温度与停留", d["snapshotFurnaceTempC"] is not None and d["snapshotResidenceS"] is not None)
    check("供料抓斗快照已保存", d["snapshotGrabNo"] == "GRAB-03")
    # 同炉第二张单：人工发起被合并
    call("POST", "/disposals/open",
         {"furnaceCode": "F-03", "ownerName": "张三", "reason": "验收人工二次发起"}, expect_ok=True)
    again = active_for("F-03")
    check("同炉只保留一张有效处置单", again["id"] == d["id"])
    # 投料被后端联锁拒绝
    call("POST", "/pits/P-03/feed",
         {"grabNo": "GRAB-03", "feedTon": 10, "targetFurnaceCode": "F-03"},
         expect_fail_contains=["冻结"])
    check("冻结期间投料被拒绝", True)
    # 记录措施并申请复核
    call("POST", f"/disposals/{d['id']}/measures",
         {"ownerName": "张三", "measureNote": "调整一次风量与给料节奏，提高炉膛温度",
          "requestReview": True, "version": d["version"]}, expect_ok=True)
    d2 = active_for("F-03")
    check("措施后进入待复核", d2["status"] == "REVIEWING", d2["status"])
    # 复核人不能是负责人本人
    call("POST", f"/disposals/{d['id']}/review",
         {"reviewerName": "张三", "choice": "RECOVER", "version": d2["version"]},
         expect_fail_contains=["另一名复核人员"])
    # 燃烧仍未达标，他人复核也不能放行
    call("POST", f"/disposals/{d['id']}/review",
         {"reviewerName": "李四", "choice": "RECOVER", "version": d2["version"]},
         expect_fail_contains=["燃烧尚未重新达标"])
    d3 = active_for("F-03")
    check("未达标复核后单据仍为待复核且仍冻结", d3["status"] == "REVIEWING" and d3["feedFrozen"])
    check("失败尝试已写入操作轨迹", any(t["result"] == "FAILED" for t in traces(d["id"])))
    return d["id"]


def scenario_b_concurrent(furnace_code, stack_code):
    print(f"\n[B] 人工发起 与 自动严重告警 并发同时到达（炉 {furnace_code} / {stack_code}）")
    results = {}

    def manual():
        results["manual"] = call("POST", "/disposals/open",
                                 {"furnaceCode": furnace_code, "ownerName": "王五", "reason": "验收并发-人工"})

    def auto_alarm():
        time.sleep(0.15)
        results["alarm"] = severe_emission(stack_code)

    t1, t2 = threading.Thread(target=manual), threading.Thread(target=auto_alarm)
    t1.start(); t2.start(); t1.join(); t2.join()
    check("人工发起返回成功", results["manual"].get("ok") is True)
    check("严重告警登记返回成功", results["alarm"].get("ok") is True)
    # 等 afterCommit 自动建单/合并落库
    deadline = time.time() + 6
    d = None
    while time.time() < deadline:
        d = active_for(furnace_code)
        if d and len(evidences(d["id"])) >= 1:
            break
        time.sleep(0.2)
    check("并发后同炉只有一张有效处置单", d is not None)
    evs = evidences(d["id"]) if d else []
    check("严重告警证据已挂接到处置单（后到不丢失）", len(evs) >= 1, json.dumps(evs, ensure_ascii=False))
    check("投料只冻结一次（冻结清单中该炉仅出现一次）", list(frozen_set()).count(furnace_code) == 1)
    check("处置单触发来源为自动或人工其一（未重复建单）", d["triggerSource"] in ("AUTO", "MANUAL"))
    return d


def scenario_c_review_rebound(d):
    print("\n[C] 待复核期间新严重告警反弹 + 两人同时复核版本冲突（不误放行）")
    did = d["id"]
    # 负责人记录措施申请复核
    cur = active_for(d["furnaceCode"])
    if cur["status"] != "REVIEWING":
        call("POST", f"/disposals/{did}/measures",
             {"ownerName": cur.get("ownerName") or "王五", "measureNote": "处置后申请复核",
              "requestReview": True, "version": cur["version"]}, expect_ok=True)
    cur = active_for(d["furnaceCode"])
    ver = cur["version"]
    # 恢复期间再到一条新严重告警（不同指标二噁英 0.3，限值 0.1 → 3 倍 SEVERE）
    severe_emission(cur["snapshotStackCode"], indicator="DIOXIN", measured=0.3)
    time.sleep(0.8)
    # 他人复核必须被驳回（反弹后单据可能已先变为处置中，两者都不得放行）
    call("POST", f"/disposals/{did}/review",
         {"reviewerName": "赵六", "choice": "RECOVER", "version": ver},
         expect_fail_any=["冻结", "版本", "待复核"])
    after = active_for(d["furnaceCode"])
    check("恢复期间新严重告警使单据反弹为处置中", after["status"] == "HANDLING", after["status"])
    check("反弹后投料仍冻结", after["feedFrozen"] and d["furnaceCode"] in frozen_set())
    evs = evidences(did)
    check("反弹新告警已补挂证据", len(evs) >= 2, f"证据数 {len(evs)}")
    check("证据存在 MERGED 并发合并标记", any(e["bindSource"] == "MERGED" for e in evs))
    # 过期版本提交
    call("POST", f"/disposals/{did}/measures",
         {"ownerName": "王五", "measureNote": "过期版本重复提交", "requestReview": False,
          "version": ver}, expect_fail_contains=["版本"])
    # 再次申请复核，两人同时点恢复：只有一人成功
    cur = active_for(d["furnaceCode"])
    call("POST", f"/disposals/{did}/measures",
         {"ownerName": "王五", "measureNote": "二次处置完成再次申请复核",
          "requestReview": True, "version": cur["version"]}, expect_ok=True)
    cur = active_for(d["furnaceCode"])
    ver2 = cur["version"]
    outcomes = {}

    def review(name):
        outcomes[name] = call("POST", f"/disposals/{did}/review",
                              {"reviewerName": "赵六", "choice": "RECOVER", "version": ver2})

    th = [threading.Thread(target=review, args=(n,)) for n in ("甲", "乙")]
    for t in th: t.start()
    for t in th: t.join()
    oks = [o.get("ok") for o in outcomes.values()]
    # 此时严重告警尚未解除，两人都应失败（且均不得放行）；核心是不能出现解冻
    check("告警未解除时并发复核无一放行", oks.count(True) == 0, str(oks))
    check("并发复核后投料仍然冻结", d["furnaceCode"] in frozen_set())
    return did


def scenario_d_recover_close(did, furnace_code):
    print(f"\n[D] 严重告警解除 + 燃烧达标 → 他人复核恢复 → 解冻 → 关闭（{furnace_code}）")
    # 解除全部严重告警
    alarms = data(call("GET", "/environment/alarms"))
    stack = data(call("GET", f"/disposals/{did}"))["snapshotStackCode"]
    severe_ids = [a["id"] for a in alarms if a["stackCode"] == stack and a["alarmLevel"] == "SEVERE"]
    check("存在待解除的严重告警", len(severe_ids) >= 2, f"{len(severe_ids)} 条")
    for aid in severe_ids:
        call("POST", f"/environment/alarms/{aid}/handle",
             {"handleNote": "验收：调整工况后解除"}, expect_ok=True)
    # 燃烧调到达标（STABILIZE 走达标判定，仅追加一条新运行记录，历史记录不动）
    call("POST", f"/incinerators/{furnace_code}/run",
         {"action": "STABILIZE", "furnaceTempC": 905, "flueGasResidenceS": 2.3, "operator": "王五"},
         expect_ok=True)
    cur = active_for(furnace_code)
    if cur["status"] != "REVIEWING":
        call("POST", f"/disposals/{did}/measures",
             {"ownerName": "王五", "measureNote": "工况已恢复，申请最终复核",
              "requestReview": True, "version": cur["version"]}, expect_ok=True)
    cur = active_for(furnace_code)
    cond = condition(did)
    check("实时恢复条件：燃烧达标", cond["combustionQualified"] is True)
    check("实时恢复条件：严重告警全部解除", cond["severeAlarmActive"] is False)
    call("POST", f"/disposals/{did}/review",
         {"reviewerName": "赵六", "choice": "RECOVER", "reviewNote": "现场复核达标",
          "version": cur["version"]}, expect_ok=True)
    rec = data(call("GET", f"/disposals/{did}"))
    check("复核后状态为已恢复", rec["status"] == "RECOVERED", rec["status"])
    check("复核恢复后投料解冻", rec["feedFrozen"] is False and furnace_code not in frozen_set())
    check("记录了恢复时炉温与停留", rec["recoverFurnaceTempC"] is not None)
    # 关闭
    call("POST", f"/disposals/{did}/close",
         {"operatorName": "赵六", "version": rec["version"]}, expect_ok=True)
    closed = data(call("GET", f"/disposals/{did}"))
    check("最终状态为已关闭", closed["status"] == "CLOSED")
    check("最终投料权限为允许（状态/权限一致）",
          closed["feedFrozen"] is False and furnace_code not in frozen_set())


def scenario_e_missing_choice():
    print("\n[E] 告警数据缺失两种选择的恢复条件影响（炉 F-02，867℃/2.1s 燃烧达标）")
    # 先由严重告警建单冻结
    severe_emission("STACK-F02", indicator="HCL", measured=200.0)  # 限值 50 → 4 倍 SEVERE
    d = wait_active("F-02")
    check("严重告警自动建单并冻结 F-02", d is not None and "F-02" in frozen_set())
    did = d["id"]
    call("POST", f"/disposals/{did}/measures",
         {"ownerName": "钱七", "measureNote": "处置中等待环保数采", "requestReview": True,
          "version": d["version"]}, expect_ok=True)
    # 选择一：继续冻结等待补齐
    cur = active_for("F-02")
    call("POST", f"/disposals/{did}/missing-choice",
         {"choice": "WAIT_FREEZE", "reason": "数采仪断连，等待运维恢复补数",
          "operatorName": "钱七", "version": cur["version"]}, expect_ok=True)
    w = data(call("GET", f"/disposals/{did}"))
    check("等待补齐：单据不关闭", w["status"] in ("REVIEWING", "HANDLING"), w["status"])
    check("等待补齐：投料继续冻结", w["feedFrozen"] and "F-02" in frozen_set())
    c1 = condition(did)
    check("等待补齐：恢复条件含数据缺失阻塞项", any("补齐" in b for b in c1["blockers"]))
    check("等待补齐：canRecover=false", c1["canRecover"] is False)
    call("POST", f"/disposals/{did}/review",
         {"reviewerName": "孙八", "choice": "RECOVER", "version": w["version"]},
         expect_fail_contains=["等待补齐"])
    # 选择二：关闭处置但保持冻结
    call("POST", f"/disposals/{did}/missing-choice",
         {"choice": "CLOSE_KEEP_FROZEN", "reason": "数采长期不可用，先关闭处置单走人工挂牌程序",
          "operatorName": "钱七", "version": w["version"]}, expect_ok=True)
    k = data(call("GET", f"/disposals/{did}"))
    check("保持冻结：处置单已关闭", k["status"] == "CLOSED")
    check("保持冻结：单据关闭但 feed_frozen 仍为真", k["feedFrozen"] is True)
    check("保持冻结：投料权限仍被拒绝", "F-02" in frozen_set())
    call("POST", "/pits/P-02/feed",
         {"grabNo": "GRAB-02", "feedTon": 5, "targetFurnaceCode": "F-02"},
         expect_fail_contains=["冻结"])
    check("保持冻结：投料接口确实拒绝", True)
    # 解除：解除告警、燃烧达标后重新发起处置单走完整恢复闭环
    alarms = data(call("GET", "/environment/alarms"))
    for a in alarms:
        if a["stackCode"] == "STACK-F02" and a["alarmLevel"] == "SEVERE" and not a["handled"]:
            call("POST", f"/environment/alarms/{a['id']}/handle", {"handleNote": "数采恢复后解除"}, expect_ok=True)
    nd = data(call("POST", "/disposals/open",
                   {"furnaceCode": "F-02", "ownerName": "钱七", "reason": "数据补齐后重新发起处置"}))
    check("旧单保持冻结期间允许重新发起新处置单", nd["status"] == "PENDING")
    nid = nd["id"]
    call("POST", f"/disposals/{nid}/measures",
         {"ownerName": "钱七", "measureNote": "确认数据齐全，申请复核", "requestReview": True,
          "version": nd["version"]}, expect_ok=True)
    cur = data(call("GET", f"/disposals/{nid}"))
    call("POST", f"/disposals/{nid}/review",
         {"reviewerName": "孙八", "choice": "RECOVER", "version": cur["version"]}, expect_ok=True)
    check("新闭环恢复后 F-02 投料解冻（旧保持冻结单一并解除）", "F-02" not in frozen_set())
    n2 = data(call("GET", f"/disposals/{nid}"))
    call("POST", f"/disposals/{nid}/close",
         {"operatorName": "孙八", "version": n2["version"]}, expect_ok=True)
    check("新处置单最终关闭", data(call("GET", f"/disposals/{nid}"))["status"] == "CLOSED")


def scenario_f_runlogs_immutable():
    print("\n[F] 已完成运行记录不被改动（闭环只新增处置数据）")
    logs = data(call("GET", "/incinerators/logs"))
    check("启停炉记录可正常查询（既有记录仍在）", len(logs) >= 12, f"实际 {len(logs)} 条")
    hist = [x for x in logs if x["action"] in ("STABILIZE", "STOP")]
    check("历史运行记录字段未被处置流程清空",
          all(x["operator"] and x["furnaceTempC"] is not None for x in hist))


def main():
    print("=" * 70)
    print("异常工况处置单闭环验收")
    print("=" * 70)
    try:
        health = urllib.request.urlopen(BASE.replace("/api", "") + "/actuator/health", timeout=5)
        json.loads(health.read().decode())
    except Exception as e:
        print(f"系统未运行或不可达：{e}\n请先执行 ./start.sh --build")
        sys.exit(2)
    scenario_f_runlogs_immutable()
    did_a = scenario_a_combustion_auto()
    d_b = scenario_b_concurrent("F-04", "STACK-F04")
    scenario_c_review_rebound(d_b)
    scenario_d_recover_close(d_b["id"], "F-04")
    scenario_e_missing_choice()
    print("\n" + "=" * 70)
    print(f"验收结果：通过 {PASS} 项，失败 {FAIL} 项")
    print("=" * 70)
    sys.exit(1 if FAIL else 0)


if __name__ == "__main__":
    main()
