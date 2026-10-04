<template>
  <div class="page">
    <!-- ===== 处置单列表 ===== -->
    <section id="sec-disposal-list" class="wp-section">
      <div class="list-head">
        <h3 class="wp-section-title">异常工况处置单 · 待处置 → 处置中 → 待复核 → 已恢复 → 已关闭</h3>
        <div class="head-actions">
          <span class="frozen-tip" v-if="frozenFurnaces.length">
            <span class="wp-dot danger"></span>
            投料冻结炉号：<b class="mono">{{ frozenFurnaces.join('、') }}</b>
          </span>
          <el-radio-group v-model="activeOnly" size="small" @change="load">
            <el-radio-button :value="true">未关闭有效单</el-radio-button>
            <el-radio-button :value="false">全部（近 50 张）</el-radio-button>
          </el-radio-group>
          <el-button size="small" @click="load">刷新</el-button>
          <el-button size="small" type="primary" @click="openDialog = true">人工发起处置单</el-button>
        </div>
      </div>

      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table disposal-table">
            <thead>
              <tr>
                <th>处置单号</th>
                <th>炉号</th>
                <th>状态</th>
                <th>来源</th>
                <th>触发原因</th>
                <th>投料联锁</th>
                <th class="right">证据告警</th>
                <th>负责人/复核人</th>
                <th>创建时间</th>
                <th class="right">版本</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="d in list"
                :key="d.id"
                :class="{ current: selected && selected.id === d.id, frozen: d.feedFrozen }"
                @click="select(d.id)"
              >
                <td class="mono strong">{{ d.disposalNo }}</td>
                <td class="mono strong">{{ d.furnaceCode }}<span class="dim"> {{ d.furnaceName }}</span></td>
                <td><span class="wp-tag" :class="statusTag(d.status)">{{ d.statusName }}</span></td>
                <td><span class="wp-tag" :class="d.triggerSource === 'AUTO' ? 'warn' : 'primary'">{{ d.triggerSourceName }}</span></td>
                <td class="reason-cell">
                  <span v-for="(r, i) in (d.triggerReasons || []).slice(0, 2)" :key="i" class="reason-line">{{ r }}</span>
                </td>
                <td>
                  <span class="wp-tag" :class="d.feedFrozen ? 'danger' : 'ok'">
                    {{ d.feedFrozen ? '冻结中' : '已解冻' }}
                  </span>
                  <span v-if="d.feedBlockedNow !== d.feedFrozen" class="stale-warn">权限以刷新为准</span>
                </td>
                <td class="right num">{{ d.alarmEvidenceCount ?? 0 }}</td>
                <td class="dim">{{ d.ownerName || '--' }} / {{ d.reviewerName || '--' }}</td>
                <td class="mono">{{ time(d.createTime) }}</td>
                <td class="right mono">v{{ d.version }}</td>
              </tr>
              <tr v-if="!list.length"><td colspan="10" class="empty">当前没有异常工况处置单</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 详情 ===== -->
    <section id="sec-disposal-detail" class="wp-section" v-if="detail">
      <h3 class="wp-section-title">
        处置详情 · <span class="mono">{{ detail.disposalNo }}</span>
        <span class="wp-tag" :class="statusTag(detail.status)">{{ detail.statusName }}</span>
        <span class="wp-tag" :class="detail.feedFrozen ? 'danger' : 'ok'">{{ detail.feedFrozen ? '投料冻结中' : '投料已恢复' }}</span>
      </h3>

      <div class="detail-grid">
        <!-- 左：快照与条件 -->
        <div class="wp-panel">
          <div class="wp-panel-title"><span>炉况 / 告警 / 供料抓斗快照（建单时冻结，不改动已完成运行记录）</span></div>
          <div class="wp-panel-body">
            <div class="kv-grid">
              <div class="kv"><span>快照炉膛温度</span><b class="num" :class="{ danger: bn(detail.snapshotFurnaceTempC) < 850 }">{{ num(detail.snapshotFurnaceTempC, 1) }} ℃</b></div>
              <div class="kv"><span>快照烟气停留</span><b class="num" :class="{ danger: bn(detail.snapshotResidenceS) < 2 }">{{ num(detail.snapshotResidenceS, 2) }} s</b></div>
              <div class="kv"><span>快照 CO</span><b class="num">{{ num(detail.snapshotCoPpm, 0) }} ppm</b></div>
              <div class="kv"><span>快照燃烧状态</span><b>{{ detail.snapshotCombustion }}</b></div>
              <div class="kv"><span>快照当日入炉量</span><b class="num">{{ num(detail.snapshotDailyTon, 1) }} t</b></div>
              <div class="kv"><span>排放口</span><b class="mono">{{ detail.snapshotStackCode || '--' }}</b></div>
              <div class="kv"><span>炉侧供料抓斗</span><b class="mono">{{ detail.snapshotGrabNo || '--' }}</b></div>
              <div class="kv"><span>供料坑位 / 抓斗</span><b class="mono">{{ detail.snapshotPitNo || '--' }} / {{ detail.snapshotPitGrabNo || '--' }}</b></div>
              <div class="kv"><span>快照坑位库存</span><b class="num">{{ num(detail.snapshotPitStockTon, 1) }} t</b></div>
              <div class="kv"><span>冻结 / 解冻时间</span><b class="mono">{{ time(detail.frozenTime) }} → {{ detail.unfrozenTime ? time(detail.unfrozenTime) : '未解冻' }}</b></div>
            </div>
            <div class="reason-box">
              <div class="rb-title">触发原因</div>
              <div v-for="(r, i) in detail.triggerReasons || []" :key="i" class="rb-item">· {{ r }}</div>
            </div>
          </div>
        </div>

        <!-- 右：实时恢复条件 -->
        <div id="sec-condition" class="wp-panel">
          <div class="wp-panel-title">
            <span>实时恢复条件（刷新/多人操作后以此为准）</span>
            <el-button size="small" text @click="loadCondition">重新评估</el-button>
          </div>
          <div class="wp-panel-body">
            <template v-if="condition">
              <div class="cond-row">
                <span>当前炉膛温度</span>
                <b class="num" :class="{ danger: !condition.combustionQualified }">{{ num(condition.currentFurnaceTempC, 1) }} ℃</b>
              </div>
              <div class="cond-row">
                <span>当前烟气停留</span>
                <b class="num" :class="{ danger: !condition.combustionQualified }">{{ num(condition.currentResidenceS, 2) }} s</b>
              </div>
              <div class="cond-row">
                <span>燃烧重新达标（≥850℃ 且 ≥2s）</span>
                <span class="wp-tag" :class="condition.combustionQualified ? 'ok' : 'danger'">{{ condition.combustionQualified ? '达标' : '未达标' }}</span>
              </div>
              <div class="cond-row">
                <span>同炉严重告警解除</span>
                <span class="wp-tag" :class="condition.severeAlarmActive ? 'danger' : 'ok'">
                  {{ condition.severeAlarmActive ? `未解除 ${condition.severeAlarmCount} 条` : '已解除' }}
                </span>
              </div>
              <div class="cond-row">
                <span>告警数据状态</span>
                <span class="wp-tag" :class="condition.alarmDataStatus === 'AVAILABLE' ? 'ok' : 'warn'">
                  {{ condition.alarmDataStatus === 'AVAILABLE' ? '数据可用' : '数据暂不可用' }}
                </span>
              </div>
              <div class="cond-row" v-if="condition.missingChoice">
                <span>数据缺失选择</span>
                <b>{{ condition.missingChoiceName }}</b>
              </div>
              <div class="cond-row">
                <span>投料权限</span>
                <span class="wp-tag" :class="condition.feedBlocked ? 'danger' : 'ok'">{{ condition.feedBlocked ? '禁止投料' : '允许投料' }}</span>
              </div>
              <div class="block-box" v-if="condition.blockers && condition.blockers.length">
                <div class="bb-title">恢复阻塞项（含恢复期间反弹新告警）</div>
                <div v-for="(b, i) in condition.blockers" :key="i" class="bb-item">✕ {{ b }}</div>
              </div>
              <div class="block-box ok-box" v-else-if="detail.status === 'REVIEWING'">
                <div class="bb-title">条件已全部满足，可由另一名复核人员执行复核恢复</div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 操作区 -->
      <div class="wp-panel action-panel">
        <div class="wp-panel-title"><span>处置操作（带版本号提交，过期版本/重复点击会被拒绝并留痕）</span></div>
        <div class="wp-panel-body action-body">
          <div class="action-info">
            当前版本 <b class="mono">v{{ detail.version }}</b> ·
            负责人 <b>{{ detail.ownerName || '未指派' }}</b> · 复核人 <b>{{ detail.reviewerName || '未复核' }}</b>
          </div>
          <div class="action-buttons">
            <el-button
              size="small" type="primary"
              :disabled="detail.status === 'CLOSED' || detail.status === 'REVIEWING' || detail.status === 'RECOVERED'"
              @click="openMeasure(false)"
            >记录措施</el-button>
            <el-button
              size="small" type="warning"
              :disabled="detail.status === 'CLOSED' || detail.status === 'REVIEWING' || detail.status === 'RECOVERED'"
              @click="openMeasure(true)"
            >记录措施并申请复核</el-button>
            <el-button
              size="small" type="success"
              :disabled="detail.status !== 'REVIEWING'"
              @click="openReview('RECOVER')"
            >复核恢复（他人）</el-button>
            <el-button
              size="small" type="info" plain
              :disabled="detail.status !== 'REVIEWING'"
              @click="openReview('REJECT')"
            >复核退回</el-button>
            <el-button size="small" :disabled="detail.status !== 'REVIEWING'" @click="openMissing">告警数据暂不可用…</el-button>
            <el-button size="small" type="danger" plain :disabled="detail.status !== 'RECOVERED'" @click="doClose">关闭处置单</el-button>
          </div>
        </div>
      </div>

      <!-- 缺失选择说明条 -->
      <div class="wp-panel missing-banner" v-if="detail.missingChoice">
        <div class="wp-panel-body">
          <span class="wp-tag warn">{{ detail.missingChoiceName }}</span>
          <span class="mb-reason">理由：{{ detail.missingReason }}（决策人 {{ detail.missingDecidedBy }}，{{ time(detail.missingDecidedTime) }}）</span>
          <span class="mb-impact" v-if="detail.missingChoice === 'WAIT_FREEZE'">影响：投料继续冻结，数据补齐后仍须燃烧达标且严重告警解除，才能复核恢复。</span>
          <span class="mb-impact" v-else>影响：处置单已关闭且投料保持冻结、不能再复核恢复；须重新发起处置单走完整闭环后方可解冻。</span>
        </div>
      </div>

      <!-- 证据 + 轨迹 -->
      <div class="detail-grid">
        <div class="wp-panel">
          <div class="wp-panel-title"><span>同炉严重排放告警证据（自动/人工并发到达会合并，后到告警不丢失）</span></div>
          <div class="wp-panel-body">
            <table class="wp-table mini-table">
              <thead>
                <tr><th>告警号</th><th>指标</th><th>实测/限值</th><th>状态</th><th>挂接方式</th><th>告警时间</th></tr>
              </thead>
              <tbody>
                <tr v-for="e in evidences" :key="e.linkId">
                  <td class="mono">{{ e.alarmNo }}</td>
                  <td>{{ e.indicatorName }}</td>
                  <td class="num">{{ num(e.measuredValue, 3) }} / {{ num(e.limitValue, 3) }}</td>
                  <td><span class="wp-tag" :class="e.handled ? 'ok' : 'danger'">{{ e.handled ? '已解除' : '未解除' }}</span></td>
                  <td><span class="wp-tag" :class="e.bindSource === 'MERGED' ? 'warn' : ''">{{ e.bindSourceName }}</span></td>
                  <td class="mono">{{ time(e.alarmTime) }}</td>
                </tr>
                <tr v-if="!evidences.length"><td colspan="6" class="empty">无严重告警证据（本次由炉况异常触发）</td></tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="wp-panel">
          <div class="wp-panel-title"><span>操作轨迹（成功与失败原因均留痕）</span></div>
          <div class="wp-panel-body trace-body">
            <div v-for="t in traces" :key="t.id" class="trace-item" :class="t.result.toLowerCase()">
              <div class="trace-head">
                <span class="trace-action">{{ traceActionName(t.action) }}</span>
                <span class="wp-tag" :class="t.result === 'SUCCESS' ? 'ok' : 'danger'">{{ t.result === 'SUCCESS' ? '成功' : '失败' }}</span>
                <span class="mono trace-time">{{ time(t.operateTime) }}</span>
                <span class="trace-op">{{ t.operatorName || '--' }}</span>
              </div>
              <div class="trace-detail">{{ t.detail }}</div>
              <div class="trace-flow dim" v-if="t.fromStatus || t.toStatus">
                {{ t.fromStatusName || '—' }} → {{ t.toStatusName || '—' }}
              </div>
            </div>
            <div v-if="!traces.length" class="empty">暂无轨迹</div>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="wp-section">
      <div class="wp-panel"><div class="wp-panel-body empty-big">请在上方列表选择一张处置单查看详情与闭环操作</div></div>
    </section>

    <!-- ===== 人工发起 ===== -->
    <el-dialog v-model="openDialog" title="人工发起异常工况处置单" width="480px">
      <el-alert type="info" :closable="false" class="dlg-alert"
        title="提交后立即保存炉况/告警/供料抓斗快照并冻结该炉投料；同炉已有有效单时自动合并证据，不重复冻结。" />
      <el-form :model="openForm" label-width="92px">
        <el-form-item label="炉号" required>
          <el-select v-model="openForm.furnaceCode" placeholder="选择焚烧炉">
            <el-option v-for="f in dict.furnaces || []" :key="f.furnaceCode"
              :label="`${f.furnaceCode} ${f.furnaceName}`" :value="f.furnaceCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人"><el-input v-model="openForm.ownerName" placeholder="默认：中控值班" /></el-form-item>
        <el-form-item label="异常说明"><el-input v-model="openForm.reason" type="textarea" :rows="2" placeholder="人工判定异常的依据" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doOpen">发起并冻结投料</el-button>
      </template>
    </el-dialog>

    <!-- ===== 措施 ===== -->
    <el-dialog v-model="measureDialog" :title="measureForm.requestReview ? '记录措施并申请复核' : '记录处置措施'" width="500px">
      <el-form :model="measureForm" label-width="92px">
        <el-form-item label="负责人" required><el-input v-model="measureForm.ownerName" /></el-form-item>
        <el-form-item label="处置措施" required>
          <el-input v-model="measureForm.measureNote" type="textarea" :rows="3" placeholder="如：加大一次风量、调整给料、提高料层厚度…" />
        </el-form-item>
        <el-alert v-if="measureForm.requestReview" type="warning" :closable="false" class="dlg-alert"
          title="申请复核后单据进入待复核，必须由另一名复核人员确认燃烧达标且严重告警解除才能恢复投料。" />
      </el-form>
      <template #footer>
        <el-button @click="measureDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doMeasure">提交（v{{ detail ? detail.version : '' }}）</el-button>
      </template>
    </el-dialog>

    <!-- ===== 复核 ===== -->
    <el-dialog v-model="reviewDialog" :title="reviewForm.choice === 'RECOVER' ? '复核恢复（须另一名复核人员）' : '复核退回处置'" width="520px">
      <el-alert
        :type="reviewForm.choice === 'RECOVER' ? 'success' : 'info'"
        :closable="false" class="dlg-alert"
        :title="reviewForm.choice === 'RECOVER'
          ? '系统将在提交时重新判定：炉膛温度 ≥850℃、停留 ≥2s、同炉严重告警全部解除；恢复期间出现新严重告警会自动驳回并反弹处置，不会误放行。'
          : '退回后单据回到处置中，投料保持冻结。'" />
      <el-form :model="reviewForm" label-width="92px">
        <el-form-item label="复核人员" required>
          <el-input v-model="reviewForm.reviewerName" :placeholder="`不得与负责人 ${detail ? detail.ownerName : ''} 为同一人`" />
        </el-form-item>
        <el-form-item label="复核意见"><el-input v-model="reviewForm.reviewNote" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button :type="reviewForm.choice === 'RECOVER' ? 'success' : 'info'" :loading="submitting" @click="doReview">
          {{ reviewForm.choice === 'RECOVER' ? '确认恢复并解冻' : '退回处置' }}（v{{ detail ? detail.version : '' }}）
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 告警数据缺失选择 ===== -->
    <el-dialog v-model="missingDialog" title="告警数据暂不可用 · 必须明确选择" width="560px">
      <el-radio-group v-model="missingForm.choice" class="missing-radio">
        <el-radio value="WAIT_FREEZE" border>
          <div class="mr-main">继续冻结等待补齐</div>
          <div class="mr-impact">恢复条件影响：状态不变、投料继续冻结；数据补齐后仍须燃烧达标且严重告警解除，才能复核恢复。</div>
        </el-radio>
        <el-radio value="CLOSE_KEEP_FROZEN" border>
          <div class="mr-main">记录理由后关闭处置但保持投料冻结</div>
          <div class="mr-impact">恢复条件影响：处置单直接关闭、投料保持冻结、不能再复核恢复；须重新发起处置单走完整闭环后方可解冻。</div>
        </el-radio>
      </el-radio-group>
      <el-input v-model="missingForm.reason" type="textarea" :rows="2" class="missing-reason" placeholder="必须填写数据缺失情况与理由" />
      <el-input v-model="missingForm.operatorName" class="missing-op" placeholder="决策人（默认负责人）" />
      <template #footer>
        <el-button @click="missingDialog = false">取消</el-button>
        <el-button type="warning" :loading="submitting" @click="doMissing">确认选择（v{{ detail ? detail.version : '' }}）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { disposalApi } from '../api'

const list = ref([])
const detail = ref(null)
const evidences = ref([])
const traces = ref([])
const condition = ref(null)
const dict = ref({})
const frozenFurnaces = ref([])
const activeOnly = ref(true)
const submitting = ref(false)

const openDialog = ref(false)
const measureDialog = ref(false)
const reviewDialog = ref(false)
const missingDialog = ref(false)

const openForm = reactive({ furnaceCode: '', ownerName: '', reason: '' })
const measureForm = reactive({ ownerName: '', measureNote: '', requestReview: false })
const reviewForm = reactive({ reviewerName: '', reviewNote: '', choice: 'RECOVER' })
const missingForm = reactive({ choice: 'WAIT_FREEZE', reason: '', operatorName: '' })

let timer = null

function num(v, d = 1) {
  if (v === null || v === undefined) return '--'
  const n = Number(v)
  if (Number.isNaN(n)) return '--'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: d, maximumFractionDigits: d })
}
function bn(v) {
  const n = Number(v)
  return Number.isNaN(n) ? 0 : n
}
function time(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '--'
}
function statusTag(s) {
  return { PENDING: 'danger', HANDLING: 'warn', REVIEWING: 'primary', RECOVERED: 'ok', CLOSED: '' }[s] || ''
}
const TRACE_ACTIONS = {
  CREATE: '建单', MERGE_ALARM: '合并告警证据', MEASURE: '记录措施', REQUEST_REVIEW: '申请复核',
  REVIEW_RECOVER: '复核恢复', REJECT: '复核退回', CLOSE: '关闭', DATA_MISSING_CHOICE: '数据缺失选择',
  FAIL: '操作失败', AUTO_SIGNAL: '自动信号'
}
function traceActionName(a) {
  return TRACE_ACTIONS[a] || a
}

async function load() {
  try {
    const [r1, r2, r3] = await Promise.all([
      disposalApi.list(activeOnly.value),
      disposalApi.frozenFurnaces(),
      disposalApi.dict()
    ])
    list.value = r1.data || []
    frozenFurnaces.value = r2.data || []
    dict.value = r3.data || {}
    if (!openForm.furnaceCode && (dict.value.furnaces || []).length) {
      openForm.furnaceCode = dict.value.furnaces[0].furnaceCode
    }
    // 多人操作后保持选中单据状态一致
    if (detail.value) {
      await refreshDetail(detail.value.id, false)
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function select(id) {
  await refreshDetail(id, true)
}

async function refreshDetail(id, scroll) {
  try {
    const [d, e, t, c] = await Promise.all([
      disposalApi.detail(id),
      disposalApi.evidences(id),
      disposalApi.traces(id),
      disposalApi.condition(id)
    ])
    detail.value = d.data
    evidences.value = e.data || []
    traces.value = t.data || []
    condition.value = c.data
    if (scroll) {
      setTimeout(() => document.querySelector('#sec-disposal-detail')?.scrollIntoView({ behavior: 'smooth' }), 60)
    }
  } catch (err) {
    ElMessage.error(err.message)
  }
}

async function loadCondition() {
  if (!detail.value) return
  try {
    const res = await disposalApi.condition(detail.value.id)
    condition.value = res.data
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function withLock(fn) {
  if (submitting.value) return
  submitting.value = true
  try {
    await fn()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
    await load()
  }
}

function doOpen() {
  if (!openForm.furnaceCode) {
    ElMessage.warning('请选择炉号')
    return
  }
  withLock(async () => {
    const res = await disposalApi.open({ ...openForm })
    ElMessage.success(res.message || '已发起')
    openDialog.value = false
    openForm.reason = ''
    if (res.data) await refreshDetail(res.data.id, true)
  })
}

function openMeasure(requestReview) {
  if (!detail.value) return
  measureForm.ownerName = detail.value.ownerName || ''
  measureForm.measureNote = detail.value.measureNote || ''
  measureForm.requestReview = requestReview
  measureDialog.value = true
}

function doMeasure() {
  if (!measureForm.ownerName || !measureForm.ownerName.trim()) {
    ElMessage.warning('请填写负责人')
    return
  }
  if (!measureForm.measureNote || !measureForm.measureNote.trim()) {
    ElMessage.warning('请填写处置措施')
    return
  }
  withLock(async () => {
    const res = await disposalApi.measure(detail.value.id, {
      ownerName: measureForm.ownerName.trim(),
      measureNote: measureForm.measureNote.trim(),
      requestReview: measureForm.requestReview,
      version: detail.value.version
    })
    ElMessage.success(res.message || '已提交')
    measureDialog.value = false
  })
}

function openReview(choice) {
  if (!detail.value) return
  reviewForm.choice = choice
  reviewForm.reviewerName = ''
  reviewForm.reviewNote = ''
  reviewDialog.value = true
}

function doReview() {
  if (!reviewForm.reviewerName || !reviewForm.reviewerName.trim()) {
    ElMessage.warning('请填写复核人员')
    return
  }
  if (reviewForm.choice === 'RECOVER'
      && reviewForm.reviewerName.trim() === (detail.value.ownerName || '').trim()) {
    ElMessage.error('复核人员不能与处置负责人为同一人')
    return
  }
  withLock(async () => {
    const res = await disposalApi.review(detail.value.id, {
      reviewerName: reviewForm.reviewerName.trim(),
      reviewNote: reviewForm.reviewNote.trim(),
      choice: reviewForm.choice,
      version: detail.value.version
    })
    ElMessage.success(res.message || '已提交复核')
    reviewDialog.value = false
  })
}

function openMissing() {
  if (!detail.value) return
  missingForm.choice = 'WAIT_FREEZE'
  missingForm.reason = ''
  missingForm.operatorName = detail.value.ownerName || ''
  missingDialog.value = true
}

function doMissing() {
  if (!missingForm.reason || !missingForm.reason.trim()) {
    ElMessage.warning('必须填写数据缺失理由')
    return
  }
  withLock(async () => {
    const res = await disposalApi.missingChoice(detail.value.id, {
      choice: missingForm.choice,
      reason: missingForm.reason.trim(),
      operatorName: (missingForm.operatorName || '').trim(),
      version: detail.value.version
    })
    ElMessage.success(res.message || '已记录选择')
    missingDialog.value = false
  })
}

function doClose() {
  withLock(async () => {
    await ElMessageBox.confirm('关闭后处置闭环结束（投料已在复核恢复时解冻），确认关闭？', '关闭处置单', { type: 'warning' })
    const res = await disposalApi.close(detail.value.id, {
      operatorName: detail.value.reviewerName || '',
      version: detail.value.version
    })
    ElMessage.success(res.message || '已关闭')
  }).catch(() => {})
}

onMounted(() => {
  load()
  // 多人操作 / 自动触发场景下定时拉取，保证列表、冻结权限状态一致
  timer = setInterval(load, 10000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.frozen-tip {
  font-size: 12px;
  color: #ffb4b0;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(239, 83, 80, 0.1);
  border: 1px solid rgba(239, 83, 80, 0.3);
}
.disposal-table tr {
  cursor: pointer;
}
.disposal-table tr.current {
  background: rgba(41, 182, 246, 0.08);
  box-shadow: inset 3px 0 0 var(--wp-accent);
}
.disposal-table tr.frozen td:first-child {
  border-left: 2px solid transparent;
}
.reason-cell {
  max-width: 260px;
}
.reason-line {
  display: block;
  font-size: 11px;
  color: var(--wp-text-dim);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stale-warn {
  display: block;
  font-size: 10px;
  color: var(--wp-warn);
  margin-top: 2px;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.kv {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 8px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border);
  font-size: 11px;
  color: var(--wp-text-dim);
}
.kv b {
  color: var(--wp-text);
  font-size: 12px;
}
.kv b.danger, .num.danger {
  color: var(--wp-danger);
}
.reason-box {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(239, 83, 80, 0.07);
  border: 1px solid rgba(239, 83, 80, 0.25);
}
.rb-title {
  font-size: 12px;
  color: #ffb4b0;
  margin-bottom: 4px;
}
.rb-item {
  font-size: 11px;
  color: var(--wp-text-dim);
  line-height: 1.6;
}
.cond-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 4px;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
  color: var(--wp-text-dim);
}
.cond-row b {
  color: var(--wp-text);
}
.block-box {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  background: rgba(239, 83, 80, 0.08);
  border: 1px solid rgba(239, 83, 80, 0.3);
}
.ok-box {
  background: rgba(102, 187, 106, 0.08);
  border-color: rgba(102, 187, 106, 0.3);
}
.bb-title {
  font-size: 12px;
  color: #ffb4b0;
  margin-bottom: 4px;
}
.ok-box .bb-title {
  color: #a5d6a7;
}
.bb-item {
  font-size: 11px;
  color: var(--wp-text-dim);
  line-height: 1.7;
}
.action-panel {
  margin-top: 12px;
}
.action-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.action-info {
  font-size: 12px;
  color: var(--wp-text-dim);
}
.action-buttons {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.missing-banner .wp-panel-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mb-reason, .mb-impact {
  font-size: 12px;
  color: var(--wp-text-dim);
}
.mb-impact {
  color: var(--wp-warn);
}
.mini-table {
  font-size: 11px;
}
.trace-body {
  max-height: 340px;
  overflow-y: auto;
}
.trace-item {
  padding: 7px 8px;
  border-left: 3px solid var(--wp-primary-3);
  background: #101a21;
  border-radius: 0 4px 4px 0;
  margin-bottom: 6px;
}
.trace-item.failed {
  border-left-color: var(--wp-danger);
  background: rgba(239, 83, 80, 0.07);
}
.trace-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.trace-action {
  font-weight: 600;
  color: var(--wp-text);
}
.trace-time {
  margin-left: auto;
  font-size: 11px;
  color: var(--wp-text-mute);
}
.trace-op {
  font-size: 11px;
  color: var(--wp-text-dim);
}
.trace-detail {
  margin-top: 3px;
  font-size: 11px;
  color: var(--wp-text-dim);
  line-height: 1.5;
}
.trace-flow {
  margin-top: 2px;
  font-size: 10px;
}
.empty-big {
  text-align: center;
  color: var(--wp-text-mute);
  padding: 40px 0;
}
.dlg-alert {
  margin-bottom: 12px;
}
.missing-radio {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.mr-main {
  font-weight: 600;
  margin-bottom: 2px;
}
.mr-impact {
  font-size: 11px;
  color: var(--wp-text-dim);
  line-height: 1.5;
}
.missing-reason {
  margin-top: 12px;
}
.missing-op {
  margin-top: 8px;
}
@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
