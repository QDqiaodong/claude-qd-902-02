<template>
  <div class="page">
    <!-- ===== 当日统计三张卡 ===== -->
    <section id="sec-stat" class="wp-section">
      <h3 class="wp-section-title">当日进厂统计</h3>
      <div class="wp-stat-grid">
        <div class="wp-stat-card">
          <div class="label">当日进厂车次</div>
          <div class="value num">{{ stat.trips ?? '--' }}<span class="unit">车次</span></div>
          <div class="foot">统计日期 {{ stat.date || '--' }}</div>
        </div>
        <div class="wp-stat-card">
          <div class="label">净重合计</div>
          <div class="value num">{{ fmt(stat.netWeightTon) }}<span class="unit">t</span></div>
          <div class="foot">净重 = 毛重 − 皮重，逐车核算</div>
        </div>
        <div class="wp-stat-card">
          <div class="label">平均净重</div>
          <div class="value num">{{ fmt(stat.avgNetWeightTon) }}<span class="unit">t/车</span></div>
          <div class="foot">地磅量程上限 {{ dict.maxNetTon ?? 45 }} t</div>
        </div>
      </div>
    </section>

    <!-- ===== 地磅流水 + 类别占比环形图 ===== -->
    <section id="sec-flow" class="wp-section">
      <h3 class="wp-section-title">进厂地磅流水（点击行可编辑，净重自动重算）</h3>
      <div class="flow-grid">
        <div class="wp-panel list-panel">
          <div class="wp-panel-title">
            <span>地磅流水 · 最近 {{ records.length }} 车次</span>
            <span class="sub">
              <el-button size="small" type="primary" @click="openCreate">新增过磅</el-button>
            </span>
          </div>
          <div class="wp-scroll">
            <table class="wp-table">
              <thead>
                <tr>
                  <th>车牌</th>
                  <th>来源</th>
                  <th>类别</th>
                  <th class="right">毛重 t</th>
                  <th class="right">皮重 t</th>
                  <th class="right">净重 t</th>
                  <th>进厂时间</th>
                  <th>地磅</th>
                  <th>卸料坑</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in records"
                  :key="row.id"
                  :class="{ active: current && current.id === row.id }"
                  @click="openEdit(row)"
                >
                  <td class="mono strong">{{ row.truckNo }}</td>
                  <td>{{ row.sourceRegion }} · {{ row.transferStation }}</td>
                  <td>
                    <span class="wp-tag" :style="catTagStyle(row.wasteCategory)">{{ row.wasteCategoryName }}</span>
                  </td>
                  <td class="right num">{{ fmt(row.grossWeightTon) }}</td>
                  <td class="right num">{{ fmt(row.tareWeightTon) }}</td>
                  <td class="right num net">{{ fmt(row.netWeightTon) }}</td>
                  <td class="mono">{{ time(row.intakeTime) }}</td>
                  <td class="mono">{{ row.scaleNo }}</td>
                  <td class="mono">{{ row.unloadPit || '--' }}</td>
                </tr>
                <tr v-if="!records.length">
                  <td colspan="9" class="empty">暂无进厂车次</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="wp-panel donut-panel">
          <div class="wp-panel-title">
            <span>按垃圾类别占比</span>
            <span class="sub">净重口径</span>
          </div>
          <div class="wp-panel-body donut-body">
            <div class="donut" :style="{ background: conic }">
              <div class="donut-hole">
                <div class="donut-total num">{{ fmt(stat.netWeightTon) }}</div>
                <div class="donut-unit">t 净重合计</div>
              </div>
            </div>
            <ul class="legend">
              <li v-for="item in stat.categories || []" :key="item.code">
                <span class="legend-dot" :style="{ background: catColor(item.code) }"></span>
                <span class="legend-name">{{ item.name }}</span>
                <span class="legend-val num">{{ fmt(item.netWeightTon) }} t</span>
                <span class="legend-rate num">{{ pct(item.rate) }}</span>
                <span class="legend-trips num">{{ item.trips }} 车</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 来源区域汇总 ===== -->
    <section id="sec-source" class="wp-section">
      <h3 class="wp-section-title">按来源行政区划汇总进厂量与热值</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>行政区划</th>
                <th class="right">车次</th>
                <th class="right">净重合计 t</th>
                <th class="right">平均热值 kJ/kg</th>
                <th style="width: 240px">进厂量占比</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in sources" :key="row.sourceRegion">
                <td class="strong">{{ row.sourceRegion }}</td>
                <td class="right num">{{ row.trips }}</td>
                <td class="right num">{{ fmt(row.netWeightTon) }}</td>
                <td class="right num">{{ row.avgCalorific ? Math.round(row.avgCalorific) : '--' }}</td>
                <td>
                  <div class="wp-bar">
                    <i :style="{ width: pct(rateOf(row.netWeightTon)) }"></i>
                  </div>
                </td>
              </tr>
              <tr v-if="!sources.length">
                <td colspan="5" class="empty">暂无汇总数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 新增 / 编辑过磅 ===== -->
    <el-dialog v-model="dialog" :title="current ? '编辑过磅记录 · ' + current.truckNo : '新增进厂过磅'" width="640px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="车牌号">
          <el-input v-model="form.truckNo" placeholder="如 粤Q·A1283" />
        </el-form-item>
        <el-form-item label="来源区域">
          <el-select v-model="form.sourceRegion" placeholder="选择行政区划" @change="form.transferStation = ''">
            <el-option v-for="r in dict.regions || []" :key="r.name" :label="r.name" :value="r.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="中转站">
          <el-select v-model="form.transferStation" placeholder="选择中转站/压缩站">
            <el-option v-for="s in stations" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="垃圾类别">
          <el-select v-model="form.wasteCategory">
            <el-option v-for="c in dict.categories || []" :key="c.code" :label="c.name" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="毛重 / 皮重">
          <div class="weight-row">
            <el-input-number v-model="form.grossWeightTon" :min="0" :precision="2" :step="0.5" />
            <span class="minus">−</span>
            <el-input-number v-model="form.tareWeightTon" :min="0" :precision="2" :step="0.5" />
            <span class="eq">=</span>
            <span class="net-preview num" :class="{ bad: previewNet <= 0 }">{{ previewNet.toFixed(2) }} t</span>
          </div>
        </el-form-item>
        <el-form-item label="地磅号">
          <el-select v-model="form.scaleNo">
            <el-option v-for="s in dict.scaleNos || []" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="热值 kJ/kg">
          <el-input-number v-model="form.calorificValue" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="驾驶员">
          <el-input v-model="form.driverName" />
        </el-form-item>
        <el-form-item label="卸料坑位">
          <el-input v-model="form.unloadPit" placeholder="如 P-01" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="有害垃圾须注明预处理措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button v-if="current" type="danger" plain @click="removeCurrent">删除</el-button>
        <el-button type="primary" @click="submit">{{ current ? '保存并重算净重' : '登记过磅' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { intakeApi } from '../api'

const records = ref([])
const stat = ref({})
const sources = ref([])
const dict = ref({})
const dialog = ref(false)
const current = ref(null)

const COLORS = {
  KITCHEN: '#2ecc8f',
  OTHER: '#29b6f6',
  RECYCLABLE: '#f5a623',
  HAZARDOUS: '#ef5350'
}

const form = reactive({
  truckNo: '',
  sourceRegion: '',
  transferStation: '',
  wasteCategory: 'OTHER',
  grossWeightTon: 30,
  tareWeightTon: 13.5,
  scaleNo: 'SCALE-01',
  calorificValue: 7500,
  driverName: '',
  unloadPit: '',
  remark: ''
})

const stations = computed(() => {
  const region = (dict.value.regions || []).find((r) => r.name === form.sourceRegion)
  return region ? region.stations : []
})

const previewNet = computed(() => {
  const gross = Number(form.grossWeightTon) || 0
  const tare = Number(form.tareWeightTon) || 0
  return Number((gross - tare).toFixed(2))
})

const conic = computed(() => {
  const list = stat.value.categories || []
  const total = list.reduce((sum, item) => sum + (Number(item.netWeightTon) || 0), 0)
  if (!total) return 'conic-gradient(#1b2833 0deg 360deg)'
  let acc = 0
  const stops = []
  list.forEach((item) => {
    const deg = (Number(item.netWeightTon) || 0) / total * 360
    const from = acc
    acc += deg
    stops.push(`${COLORS[item.code] || '#546e7a'} ${from}deg ${acc}deg`)
  })
  return `conic-gradient(${stops.join(', ')})`
})

function catColor(code) {
  return COLORS[code] || '#546e7a'
}

function catTagStyle(code) {
  const color = COLORS[code] || '#546e7a'
  return { color, borderColor: color + '66', background: color + '1a' }
}

function fmt(value) {
  if (value === null || value === undefined) return '--'
  return Number(value).toFixed(2)
}

function pct(rate) {
  return ((rate || 0) * 100).toFixed(1) + '%'
}

function time(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(5, 16)
}

function rateOf(netTon) {
  const max = Math.max(...sources.value.map((s) => Number(s.netWeightTon) || 0), 1)
  return (Number(netTon) || 0) / max
}

async function load() {
  try {
    const [r1, r2, r3, r4] = await Promise.all([
      intakeApi.records(),
      intakeApi.dailyStat(),
      intakeApi.sourceSummary(),
      intakeApi.dict()
    ])
    records.value = r1.data || []
    stat.value = r2.data || {}
    sources.value = r3.data || []
    dict.value = r4.data || {}
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  current.value = null
  Object.assign(form, {
    truckNo: '',
    sourceRegion: (dict.value.regions && dict.value.regions[0] && dict.value.regions[0].name) || '',
    transferStation: '',
    wasteCategory: 'OTHER',
    grossWeightTon: 30,
    tareWeightTon: 13.5,
    scaleNo: (dict.value.scaleNos && dict.value.scaleNos[0]) || 'SCALE-01',
    calorificValue: 7500,
    driverName: '',
    unloadPit: '',
    remark: ''
  })
  if (stations.value.length) form.transferStation = stations.value[0]
  dialog.value = true
}

function openEdit(row) {
  current.value = row
  Object.assign(form, {
    truckNo: row.truckNo,
    sourceRegion: row.sourceRegion,
    transferStation: row.transferStation,
    wasteCategory: row.wasteCategory,
    grossWeightTon: Number(row.grossWeightTon),
    tareWeightTon: Number(row.tareWeightTon),
    scaleNo: row.scaleNo,
    calorificValue: Number(row.calorificValue),
    driverName: row.driverName,
    unloadPit: row.unloadPit,
    remark: row.remark
  })
  dialog.value = true
}

async function submit() {
  if (previewNet.value <= 0) {
    ElMessage.error(`净重必须为正：毛重 ${form.grossWeightTon} t − 皮重 ${form.tareWeightTon} t = ${previewNet.value} t`)
    return
  }
  try {
    if (current.value) {
      await intakeApi.update(current.value.id, { ...form })
      ElMessage.success('过磅记录已更新，净重已重算')
    } else {
      await intakeApi.create({ ...form })
      ElMessage.success('过磅登记成功')
    }
    dialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function removeCurrent() {
  try {
    await intakeApi.remove(current.value.id)
    ElMessage.success('过磅记录已删除')
    dialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.flow-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
}

.list-panel,
.donut-panel {
  min-width: 0;
}

.wp-table td.right,
.wp-table th.right {
  text-align: right;
}

.wp-table td.strong,
.wp-table td.mono.strong {
  color: #eaf6fc;
  font-weight: 600;
}

.wp-table td.net {
  color: var(--wp-accent);
  font-weight: 700;
}

.empty {
  text-align: center;
  color: var(--wp-text-mute);
  padding: 22px 0 !important;
}

/* 环形图 */
.donut-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.donut {
  position: relative;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  box-shadow: 0 0 0 1px var(--wp-border-2), 0 10px 26px rgba(0, 0, 0, 0.5);
}

.donut-hole {
  position: absolute;
  inset: 34px;
  border-radius: 50%;
  background: #121c24;
  border: 1px solid var(--wp-border);
  display: grid;
  place-content: center;
  text-align: center;
}

.donut-total {
  font-size: 22px;
  font-weight: 700;
  color: #eaf6fc;
}

.donut-unit {
  font-size: 11px;
  color: var(--wp-text-mute);
}

.legend {
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
}

.legend li {
  display: grid;
  grid-template-columns: 10px 62px 1fr 46px 44px;
  align-items: center;
  gap: 6px;
  padding: 5px 0;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
}

.legend-dot {
  width: 9px;
  height: 9px;
  border-radius: 2px;
}

.legend-name {
  color: var(--wp-text-dim);
}

.legend-val,
.legend-rate,
.legend-trips {
  text-align: right;
  color: var(--wp-text);
}

.legend-trips {
  color: var(--wp-text-mute);
}

.weight-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.weight-row .minus,
.weight-row .eq {
  color: var(--wp-text-mute);
}

.net-preview {
  min-width: 86px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border-2);
  color: var(--wp-ok);
  font-weight: 700;
  text-align: center;
}

.net-preview.bad {
  color: var(--wp-danger);
  border-color: rgba(239, 83, 80, 0.5);
}
</style>
