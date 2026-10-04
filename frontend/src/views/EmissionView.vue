<template>
  <div class="page">
    <!-- ===== 六项污染物半圆仪表 ===== -->
    <section id="sec-gauges" class="wp-section">
      <h3 class="wp-section-title">
        环保排放仪表盘（实测值 / 限值）
        <el-button size="small" type="primary" @click="openEmission">登记排放监测值</el-button>
      </h3>
      <div class="gauge-grid">
        <div
          v-for="g in gauges"
          :key="g.indicatorCode"
          class="wp-panel gauge-card"
          :class="{ shake: g.exceeded }"
        >
          <div class="gauge-head">
            <span class="g-name">{{ g.indicatorName }}</span>
            <span class="wp-tag" :class="g.exceeded ? 'danger' : 'ok'">{{ g.exceeded ? '超标' : '达标' }}</span>
          </div>
          <svg viewBox="0 0 220 134" class="gauge-svg">
            <!-- 底弧 -->
            <path :d="arcPath" class="g-track" />
            <!-- 限值刻度 -->
            <g>
              <line v-for="t in ticks" :key="t.r" :x1="t.x1" :y1="t.y1" :x2="t.x2" :y2="t.y2" class="g-tick" />
            </g>
            <!-- 限值红线 -->
            <line :x1="limitMark.x1" :y1="limitMark.y1" :x2="limitMark.x2" :y2="limitMark.y2" class="g-limit" />
            <!-- 实测弧 -->
            <path :d="arcPath" class="g-value" :class="gaugeClass(g)" :style="arcStyle(g)" />
            <!-- 指针 -->
            <line :x1="110" :y1="112" :x2="needle(g).x" :y2="needle(g).y" class="g-needle" :class="gaugeClass(g)" />
            <circle cx="110" cy="112" r="5" class="g-hub" />
            <text x="110" y="130" class="g-scale">0</text>
            <text x="14" y="130" class="g-scale">限值 {{ g.limitValue }}</text>
            <text x="206" y="130" class="g-scale end">×1.2</text>
          </svg>
          <div class="gauge-read">
            <span class="read-value num" :class="gaugeClass(g)">{{ g.measuredValue }}</span>
            <span class="read-unit">{{ g.unit }}</span>
          </div>
          <div class="gauge-foot">
            <span>限值 {{ g.limitValue }} {{ g.unit }}</span>
            <span :class="g.exceeded ? 'bad' : 'good'">
              占标 {{ pct(g.gaugeRatio) }}<template v-if="g.exceeded"> · 超标 {{ g.exceedRatio }}%</template>
            </span>
          </div>
          <div class="gauge-meta mono">{{ g.periodLabel || '--' }} · {{ g.stackCode || '--' }}</div>
        </div>
      </div>
    </section>

    <!-- ===== 超标告警 ===== -->
    <section id="sec-alarms" class="wp-section">
      <h3 class="wp-section-title">环保告警（超标自动登记）</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <div v-if="!alarms.length" class="empty-box">当前无未处理告警</div>
          <div v-for="a in alarms" :key="a.id" class="alarm-row">
            <span class="wp-tag" :class="levelTag(a.alarmLevel)">{{ a.alarmLevelName }}</span>
            <span class="a-name">{{ a.indicatorName }}</span>
            <span class="a-val num">{{ a.measuredValue }} / 限值 {{ a.limitValue }} {{ a.unit || '' }}</span>
            <span class="a-ratio num">超标 {{ a.exceedRatio }}%</span>
            <span class="a-period">{{ a.periodLabel }}</span>
            <span class="a-stack mono">{{ a.stackCode }}</span>
            <el-button size="small" type="primary" plain @click="handleAlarm(a)">确认处理</el-button>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 超标次数排行 ===== -->
    <section id="sec-rank" class="wp-section">
      <h3 class="wp-section-title">排放指标超标次数排行</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <div v-for="(r, i) in rank" :key="r.indicatorCode" class="rank-row">
            <span class="rank-no mono">{{ String(i + 1).padStart(2, '0') }}</span>
            <span class="rank-name">{{ r.indicatorName }}</span>
            <div class="wp-bar danger"><i :style="{ width: pct((r.exceedCount || 0) / maxRank) }"></i></div>
            <span class="rank-count num">{{ r.exceedCount }} 次</span>
            <span class="rank-max num">峰值 {{ r.maxMeasuredValue }} / 限值 {{ r.limitValue }}</span>
          </div>
          <div v-if="!rank.length" class="empty-box">暂无超标记录</div>
        </div>
      </div>
    </section>

    <!-- ===== 灰渣与渗滤液台账 ===== -->
    <section id="sec-ash" class="wp-section">
      <h3 class="wp-section-title">灰渣与渗滤液台账</h3>
      <div class="wp-panel">
        <div class="wp-panel-title">
          <span>近 15 天台账</span>
          <span class="sub">
            飞灰合计 <b class="num">{{ fmt(dash.flyAshTotalTon) }}</b> t ·
            炉渣合计 <b class="num">{{ fmt(dash.bottomAshTotalTon) }}</b> t ·
            渗滤液处理合计 <b class="num">{{ fmt(dash.leachateTotalM3) }}</b> m³
          </span>
        </div>
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>班次</th>
                <th class="right">飞灰 t</th>
                <th class="right">炉渣 t</th>
                <th class="right">固化量 t</th>
                <th>固化后去向</th>
                <th class="right">渗滤液处理 m³</th>
                <th class="right">出水 COD</th>
                <th>出水</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in ashRecords" :key="a.id">
                <td class="mono strong">{{ a.recordDate }}</td>
                <td>{{ a.shift }}</td>
                <td class="right num">{{ fmt(a.flyAshTon) }}</td>
                <td class="right num">{{ fmt(a.bottomAshTon) }}</td>
                <td class="right num">{{ fmt(a.solidifiedTon) }}</td>
                <td>{{ a.disposalDest }}</td>
                <td class="right num">{{ fmt(a.leachateTreatedM3) }}</td>
                <td class="right num" :class="{ bad: !a.effluentOk }">{{ fmt(a.effluentCod) }}</td>
                <td><span class="wp-tag" :class="a.effluentOk ? 'ok' : 'danger'">{{ a.effluentOk ? '达标' : '超标' }}</span></td>
                <td class="dim">{{ a.remark || '--' }}</td>
              </tr>
              <tr v-if="!ashRecords.length"><td colspan="10" class="empty">暂无台账</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 当日产出登记 ===== -->
    <section id="sec-ash-form" class="wp-section">
      <h3 class="wp-section-title">登记当日灰渣与渗滤液产出</h3>
      <div class="wp-panel ash-form-panel">
        <div class="wp-panel-body">
          <el-form :model="ash" label-width="120px" inline>
            <el-form-item label="日期"><el-date-picker v-model="ash.recordDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
            <el-form-item label="班次">
              <el-select v-model="ash.shift" style="width: 96px">
                <el-option label="白班" value="白班" /><el-option label="夜班" value="夜班" />
              </el-select>
            </el-form-item>
            <el-form-item label="飞灰 t"><el-input-number v-model="ash.flyAshTon" :min="0" :precision="2" :step="1" /></el-form-item>
            <el-form-item label="炉渣 t"><el-input-number v-model="ash.bottomAshTon" :min="0" :precision="2" :step="10" /></el-form-item>
            <el-form-item label="固化量 t"><el-input-number v-model="ash.solidifiedTon" :min="0" :precision="2" :step="1" /></el-form-item>
            <el-form-item label="固化后去向">
              <el-select v-model="ash.disposalDest">
                <el-option v-for="d in envDict.ashDisposal || []" :key="d" :label="d" :value="d" />
              </el-select>
            </el-form-item>
            <el-form-item label="渗滤液 m³"><el-input-number v-model="ash.leachateTreatedM3" :min="0" :precision="2" :step="10" /></el-form-item>
            <el-form-item label="出水 COD"><el-input-number v-model="ash.effluentCod" :min="0" :precision="2" :step="1" /></el-form-item>
            <el-form-item label="备注"><el-input v-model="ash.remark" /></el-form-item>
            <el-form-item><el-button type="primary" @click="submitAsh">登记产出</el-button></el-form-item>
          </el-form>
          <div class="hint">出水 COD 限值 {{ envDict.effluentCodLimit }} mg/L，超出将被标记为不达标；固化量不得大于飞灰产生量。</div>
        </div>
      </div>
    </section>

    <!-- ===== 排放登记弹窗 ===== -->
    <el-dialog v-model="emissionDialog" title="登记排放监测值" width="480px">
      <el-form :model="emission" label-width="110px">
        <el-form-item label="排放指标">
          <el-select v-model="emission.indicatorCode">
            <el-option v-for="i in envDict.indicators || []" :key="i.code" :label="`${i.name}（限值 ${i.limit} ${i.unit}）`" :value="i.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="实测值">
          <el-input-number v-model="emission.measuredValue" :min="0" :precision="3" :step="1" />
        </el-form-item>
        <el-form-item label="监测时段">
          <el-input v-model="emission.periodLabel" placeholder="如 当日 18:00~24:00 时段" />
        </el-form-item>
        <el-form-item label="排放口（炉）">
          <el-select v-model="emission.stackCode">
            <el-option v-for="f in disposalFurnaces" :key="f.stackCode"
              :label="`${f.stackCode} · ${f.furnaceCode} ${f.furnaceName}`" :value="f.stackCode" />
          </el-select>
          <div class="hint">严重告警（≥限值 2 倍）会按排放口归属同炉并自动冻结该炉投料。</div>
        </el-form-item>
        <el-form-item label="排放标记">
          <el-select v-model="emission.emissionFlag">
            <el-option v-for="f in envDict.emissionFlags || []" :key="f.code" :label="f.name" :value="f.code" />
          </el-select>
        </el-form-item>
        <div class="hint">实测值超限值将自动登记环保告警，且不允许标记为「正常排放」。</div>
      </el-form>
      <template #footer>
        <el-button @click="emissionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEmission">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { envApi, disposalApi } from '../api'

const R = 88
const CX = 110
const CY = 112
const MAX_RATIO = 1.2
const ARC_LEN = Math.PI * R

const dash = ref({})
const alarms = ref([])
const rank = ref([])
const ashRecords = ref([])
const gauges = ref([])
const envDict = ref({})
const disposalFurnaces = ref([])

const emissionDialog = ref(false)
const emission = reactive({
  indicatorCode: 'NOX',
  measuredValue: 200,
  periodLabel: '',
  stackCode: 'STACK-F01',
  emissionFlag: 'NORMAL'
})

const ash = reactive({
  recordDate: '',
  shift: '白班',
  flyAshTon: 68,
  bottomAshTon: 512,
  solidifiedTon: 62,
  disposalDest: '',
  leachateTreatedM3: 386,
  effluentCod: 78,
  remark: ''
})

const arcPath = `M ${CX - R} ${CY} A ${R} ${R} 0 0 1 ${CX + R} ${CY}`

const maxRank = computed(() => Math.max(...rank.value.map((r) => Number(r.exceedCount) || 0), 1))

function polar(frac, radius) {
  const angle = Math.PI * frac
  return { x: CX - radius * Math.cos(angle), y: CY - radius * Math.sin(angle) }
}

const ticks = computed(() =>
  [0, 0.25, 0.5, 0.75, 1.0, MAX_RATIO].map((r) => {
    const frac = r / MAX_RATIO
    const inner = polar(frac, R - 10)
    const outer = polar(frac, R - 2)
    return { r, x1: inner.x, y1: inner.y, x2: outer.x, y2: outer.y }
  })
)

const limitMark = computed(() => {
  const frac = 1 / MAX_RATIO
  const inner = polar(frac, R - 16)
  const outer = polar(frac, R + 8)
  return { x1: inner.x, y1: inner.y, x2: outer.x, y2: outer.y }
})

function fracOf(g) {
  const ratio = Number(g.gaugeRatio) || 0
  return Math.min(Math.max(ratio, 0), MAX_RATIO) / MAX_RATIO
}

function arcStyle(g) {
  const frac = fracOf(g)
  return {
    strokeDasharray: `${ARC_LEN}`,
    strokeDashoffset: `${ARC_LEN * (1 - frac)}`
  }
}

function needle(g) {
  return polar(fracOf(g), R - 18)
}

function gaugeClass(g) {
  const ratio = Number(g.gaugeRatio) || 0
  if (ratio > 1) return 'danger'
  if (ratio > 0.8) return 'warn'
  return 'ok'
}

function levelTag(level) {
  if (level === 'SEVERE') return 'danger'
  if (level === 'MAJOR') return 'warn'
  return 'primary'
}

function fmt(v) {
  return v === null || v === undefined ? '--' : Number(v).toFixed(2)
}

function pct(rate) {
  return ((rate || 0) * 100).toFixed(1) + '%'
}

async function load() {
  try {
    const [r1, r2, r3, r4, r5] = await Promise.all([
      envApi.dashboard(),
      envApi.alarms(),
      envApi.exceedRank(),
      envApi.dict(),
      disposalApi.dict()
    ])
    dash.value = r1.data || {}
    gauges.value = dash.value.gauges || []
    ashRecords.value = dash.value.ashRecords || []
    rank.value = dash.value.rank || []
    alarms.value = r2.data || []
    envDict.value = r4.data || {}
    disposalFurnaces.value = (r5.data && r5.data.furnaces) || []
    if (!disposalFurnaces.value.some((f) => f.stackCode === emission.stackCode) && disposalFurnaces.value.length) {
      emission.stackCode = disposalFurnaces.value[0].stackCode
    }
    if (!ash.disposalDest && envDict.value.ashDisposal && envDict.value.ashDisposal.length) {
      ash.disposalDest = envDict.value.ashDisposal[0]
    }
    if (!ash.recordDate) ash.recordDate = new Date().toISOString().slice(0, 10)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openEmission() {
  emissionDialog.value = true
}

async function submitEmission() {
  try {
    const res = await envApi.register({ ...emission })
    ElMessage.success(res.message || '已登记')
    emissionDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleAlarm(a) {
  try {
    const { value } = await ElMessageBox.prompt('填写处理说明', '确认处理环保告警', {
      inputValue: '已调整工况并复核表码',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    const res = await envApi.handleAlarm(a.id, value)
    ElMessage.success(res.message || '已处理')
    await load()
  } catch (e) {
    if (e && e.message) ElMessage.error(e.message)
  }
}

async function submitAsh() {
  try {
    const res = await envApi.registerAsh({ ...ash })
    ElMessage.success(res.message || '已登记')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.gauge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.gauge-card {
  padding: 10px 12px 12px;
}

.gauge-card.shake {
  animation: wp-shake 0.45s ease-in-out infinite;
  border-color: rgba(239, 83, 80, 0.55);
}

.gauge-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.g-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--wp-text);
}

.gauge-svg {
  width: 100%;
  height: auto;
}

.g-track {
  fill: none;
  stroke: #16222b;
  stroke-width: 14;
  stroke-linecap: round;
}

.g-value {
  fill: none;
  stroke: var(--wp-ok);
  stroke-width: 14;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.6s ease;
}

.g-value.ok {
  stroke: var(--wp-ok);
}

.g-value.warn {
  stroke: var(--wp-warn);
}

.g-value.danger {
  stroke: var(--wp-danger);
}

.g-tick {
  stroke: #3c5566;
  stroke-width: 1.2;
}

.g-limit {
  stroke: var(--wp-warn);
  stroke-width: 2;
  stroke-dasharray: 3 2;
}

.g-needle {
  stroke: #eaf6fc;
  stroke-width: 2.4;
  stroke-linecap: round;
}

.g-needle.danger {
  stroke: var(--wp-danger);
}

.g-needle.warn {
  stroke: var(--wp-warn);
}

.g-hub {
  fill: #263238;
  stroke: #78909c;
  stroke-width: 1.4;
}

.g-scale {
  fill: #6b8290;
  font-size: 9px;
  text-anchor: middle;
}

.g-scale.end {
  text-anchor: end;
}

.gauge-read {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  margin-top: -6px;
}

.read-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--wp-ok);
}

.read-value.warn {
  color: var(--wp-warn);
}

.read-value.danger {
  color: var(--wp-danger);
}

.read-unit {
  font-size: 11px;
  color: var(--wp-text-mute);
}

.gauge-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--wp-border);
  font-size: 11px;
  color: var(--wp-text-mute);
}

.gauge-foot .bad {
  color: var(--wp-danger);
}

.gauge-foot .good {
  color: var(--wp-ok);
}

.gauge-meta {
  margin-top: 3px;
  text-align: center;
  font-size: 10px;
  color: var(--wp-text-mute);
}

/* 告警 */
.alarm-row {
  display: grid;
  grid-template-columns: 52px 110px 1fr 96px 150px 84px 88px;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
}

.a-name {
  color: #eaf6fc;
  font-weight: 600;
}

.a-val,
.a-ratio {
  color: var(--wp-text);
}

.a-ratio {
  color: var(--wp-danger);
  font-weight: 600;
}

.a-period,
.a-stack {
  color: var(--wp-text-mute);
}

/* 排行 */
.rank-row {
  display: grid;
  grid-template-columns: 30px 130px 1fr 70px 200px;
  align-items: center;
  gap: 10px;
  padding: 7px 0;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
}

.rank-no {
  color: var(--wp-accent);
  font-weight: 700;
}

.rank-name {
  color: var(--wp-text);
}

.rank-count {
  text-align: right;
  color: var(--wp-danger);
  font-weight: 600;
}

.rank-max {
  text-align: right;
  color: var(--wp-text-mute);
}

.wp-table td.right,
.wp-table th.right {
  text-align: right;
}

.wp-table td.strong {
  color: #eaf6fc;
  font-weight: 600;
}

.wp-table td.dim,
.wp-table td.dim {
  color: var(--wp-text-mute);
}

.wp-table td.bad {
  color: var(--wp-danger);
}

.empty,
.empty-box {
  text-align: center;
  color: var(--wp-text-mute);
  padding: 20px 0 !important;
}

.ash-form-panel .hint,
.hint {
  margin-top: 8px;
  padding: 7px 10px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border);
  font-size: 11px;
  color: var(--wp-text-dim);
}
</style>
