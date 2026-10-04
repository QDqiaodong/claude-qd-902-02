<template>
  <div class="page">
    <!-- ===== 储坑侧视剖面 ===== -->
    <section id="sec-profile" class="wp-section">
      <h3 class="wp-section-title">垃圾储坑侧视剖面（点击坑位弹出抓斗投料面板）</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <svg viewBox="0 0 940 402" class="pit-svg">
            <defs>
              <pattern id="grainPat" width="11" height="11" patternUnits="userSpaceOnUse">
                <circle cx="2" cy="2" r="1.2" fill="#000000" opacity="0.4" />
                <circle cx="7.5" cy="6" r="1.5" fill="#000000" opacity="0.28" />
                <circle cx="4" cy="8.5" r="0.9" fill="#ffffff" opacity="0.14" />
                <circle cx="9" cy="2" r="0.8" fill="#c9a227" opacity="0.22" />
              </pattern>
              <linearGradient id="heapGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#9a7b51" />
                <stop offset="55%" stop-color="#6b5333" />
                <stop offset="100%" stop-color="#3a2f20" />
              </linearGradient>
              <linearGradient id="shellGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#18242d" />
                <stop offset="100%" stop-color="#0e171d" />
              </linearGradient>
            </defs>

            <!-- 卸料平台与抓斗轨道 -->
            <text x="24" y="24" class="svg-note">卸料平台 / 抓斗起重机轨道</text>
            <rect x="20" y="32" width="900" height="5" fill="#2b3944" />
            <line x1="20" y1="46" x2="920" y2="46" stroke="#22303b" stroke-dasharray="6 5" />
            <g v-if="selected">
              <rect :x="selected.geo.cx - 16" y="38" width="32" height="12" rx="2" fill="#37474f" stroke="#546e7a" />
              <line :x1="selected.geo.cx" y1="50" :x2="selected.geo.cx" :y2="selected.geo.yt + 6" stroke="#78909c" stroke-width="1" />
              <path
                :d="`M ${selected.geo.cx - 12} ${selected.geo.yt + 6} L ${selected.geo.cx - 4} ${selected.geo.yt + 22} L ${selected.geo.cx + 4} ${selected.geo.yt + 22} L ${selected.geo.cx + 12} ${selected.geo.yt + 6} Z`"
                fill="#4d6572" stroke="#8fa3ae"
              />
            </g>

            <!-- 坑体 -->
            <g
              v-for="bay in bays"
              :key="bay.pitNo"
              class="bay"
              :class="{ selected: selected && selected.pitNo === bay.pitNo }"
              @click="openFeed(bay)"
            >
              <polygon :points="bay.geo.shell" fill="url(#shellGrad)" :stroke="bay.stroke" stroke-width="1.6" />
              <path :d="bay.geo.heapPath" fill="url(#heapGrad)" />
              <path :d="bay.geo.heapPath" fill="url(#grainPat)" />
              <!-- 渗滤液收集池 -->
              <rect :x="bay.geo.x + 20" y="331" :width="100" height="9" fill="#1c5c78" opacity="0.85" />
              <text :x="bay.geo.cx" y="338" class="svg-leachate">渗滤液</text>

              <text :x="bay.geo.cx" y="62" class="svg-pitno">{{ bay.pitNo }}</text>
              <circle :cx="bay.geo.cx - 26" cy="58" r="3.5" :fill="bay.dot" />
              <text :x="bay.geo.cx" y="300" class="svg-inner">堆酵 {{ num(bay.fermentationDays, 1) }} d</text>
              <text :x="bay.geo.cx" y="316" class="svg-inner">坑温 {{ num(bay.pitTempC, 1) }} ℃</text>
              <text :x="bay.geo.cx" y="356" class="svg-stock">
                {{ fmt(bay.currentStockTon) }} / {{ fmt(bay.capacityTon) }} t
              </text>
              <text :x="bay.geo.cx" y="372" class="svg-rate" :fill="bay.dot">{{ pct(bay.fillRate) }}</text>
              <text :x="bay.geo.cx" y="390" class="svg-note-small">{{ bay.statusName }}</text>
            </g>
          </svg>
          <div class="pit-legend">
            <span v-for="bay in bays" :key="bay.pitNo" class="pit-chip" @click="openFeed(bay)">
              <span class="wp-dot" :class="dotClass(bay)"></span>{{ bay.pitNo }} {{ bay.fillRate ? pct(bay.fillRate) : '0%' }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 抓斗投料 ===== -->
    <section id="sec-feed" class="wp-section">
      <h3 class="wp-section-title">抓斗投料 / 卸料入坑</h3>
      <div class="feed-grid">
        <div class="wp-panel">
          <div class="wp-panel-title"><span>抓斗投料</span><span class="sub">投料量计入目标炉当班入炉量</span></div>
          <div class="wp-panel-body">
            <el-form :model="feed" label-width="92px">
              <el-form-item label="储坑">
                <el-select v-model="feed.pitNo" placeholder="选择储坑">
                  <el-option v-for="p in pits" :key="p.pitNo" :label="`${p.pitNo} ${p.pitName}（余 ${fmt(p.freeTon)} t）`" :value="p.pitNo" />
                </el-select>
              </el-form-item>
              <el-form-item label="抓斗">
                <el-select v-model="feed.grabNo">
                  <el-option v-for="g in dict.grabNos || []" :key="g" :label="g" :value="g" />
                </el-select>
              </el-form-item>
              <el-form-item label="投料量 t">
                <el-input-number v-model="feed.feedTon" :min="0" :precision="2" :step="5" />
              </el-form-item>
              <el-form-item label="目标炉号">
                <el-select v-model="feed.targetFurnaceCode">
                  <el-option v-for="f in dict.furnaceCodes || []" :key="f" :label="f + (frozenSet.has(f) ? '（投料冻结中）' : '')" :value="f">
                    <span>{{ f }}</span>
                    <span v-if="frozenSet.has(f)" style="float:right;color:#ef5350;font-size:11px">⛔ 冻结</span>
                  </el-option>
                </el-select>
              </el-form-item>
              <el-alert
                v-if="feed.targetFurnaceCode && frozenSet.has(feed.targetFurnaceCode)"
                type="error" :closable="false" show-icon class="feed-freeze-alert"
                title="目标炉存在未恢复的异常工况处置单，投料已联锁冻结，执行会被拒绝；须另一名复核人员复核恢复后方可投料。" />
              <el-form-item>
                <el-button type="primary" :disabled="feed.targetFurnaceCode && frozenSet.has(feed.targetFurnaceCode)" @click="doFeed">执行投料</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="wp-panel">
          <div class="wp-panel-title"><span>卸料入坑</span><span class="sub">库存超容量将拒绝卸料</span></div>
          <div class="wp-panel-body">
            <el-form :model="unload" label-width="92px">
              <el-form-item label="储坑">
                <el-select v-model="unload.pitNo">
                  <el-option v-for="p in pits" :key="p.pitNo" :label="`${p.pitNo}（余 ${fmt(p.freeTon)} t）`" :value="p.pitNo" />
                </el-select>
              </el-form-item>
              <el-form-item label="车牌号">
                <el-input v-model="unload.truckNo" placeholder="粤Q·A1283" />
              </el-form-item>
              <el-form-item label="卸料量 t">
                <el-input-number v-model="unload.unloadTon" :min="0" :precision="2" :step="2" />
              </el-form-item>
              <el-form-item label="垃圾类别">
                <el-select v-model="unload.wasteCategory">
                  <el-option label="厨余垃圾" value="KITCHEN" />
                  <el-option label="其他垃圾" value="OTHER" />
                  <el-option label="可回收物" value="RECYCLABLE" />
                  <el-option label="有害垃圾" value="HAZARDOUS" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="doUnload">确认卸料</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 渗滤液与堆酵 ===== -->
    <section id="sec-leachate" class="wp-section">
      <h3 class="wp-section-title">渗滤液抽出与堆酵台账</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>坑位</th>
                <th class="right">容量 t</th>
                <th class="right">库存 t</th>
                <th class="right">可容纳余量 t</th>
                <th style="width: 160px">库存率</th>
                <th class="right">堆酵 d</th>
                <th class="right">坑温 ℃</th>
                <th class="right">渗滤液 m³</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in pits" :key="p.pitNo">
                <td class="strong">{{ p.pitNo }} {{ p.pitName }}</td>
                <td class="right num">{{ fmt(p.capacityTon) }}</td>
                <td class="right num">{{ fmt(p.currentStockTon) }}</td>
                <td class="right num">{{ fmt(p.freeTon) }}</td>
                <td>
                  <div class="wp-bar" :class="barClass(p)"><i :style="{ width: pct(p.fillRate) }"></i></div>
                </td>
                <td class="right num">{{ num(p.fermentationDays, 1) }}</td>
                <td class="right num">{{ num(p.pitTempC, 1) }}</td>
                <td class="right num">{{ fmt(p.leachatePumpedM3) }}</td>
                <td><span class="wp-tag" :class="tagClass(p)">{{ p.statusName }}</span></td>
                <td>
                  <el-button size="small" @click="doPump(p)">抽出 20 m³</el-button>
                  <el-button size="small" type="primary" plain @click="doFerment(p)">堆酵 +0.5 d</el-button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pitApi, disposalApi } from '../api'

const pits = ref([])
const dict = ref({})
const selected = ref(null)
const frozenFurnaces = ref([])
const frozenSet = computed(() => new Set(frozenFurnaces.value || []))

const feed = reactive({ pitNo: '', grabNo: 'GRAB-01', feedTon: 25, targetFurnaceCode: '' })
const unload = reactive({ pitNo: '', truckNo: '粤Q·A1283', unloadTon: 16, wasteCategory: 'OTHER' })

const TOP = 70
const BOT = 340
const W = 140
const GAP = 8

const bays = computed(() =>
  pits.value.map((pit, index) => {
    const x = 30 + index * (W + GAP)
    const cx = x + W / 2
    const capacity = Number(pit.capacityTon) || 0
    const stock = Number(pit.currentStockTon) || 0
    const rate = capacity ? Math.min(stock / capacity, 1) : 0
    const h = rate * (BOT - TOP)
    const yt = BOT - h
    const inset = (y) => 18 * ((y - TOP) / (BOT - TOP))
    const lx = x + inset(yt)
    const rx = x + W - inset(yt)
    const shell = `${x},${TOP} ${x + W},${TOP} ${x + W - 18},${BOT} ${x + 18},${BOT}`
    const heapPath = h <= 0.5
      ? ''
      : `M ${lx} ${yt} Q ${cx} ${yt - 11} ${rx} ${yt} L ${x + W - 18} ${BOT} L ${x + 18} ${BOT} Z`
    return {
      ...pit,
      geo: { x, cx, yt, shell, heapPath },
      stroke: strokeOf(pit),
      dot: dotColorOf(pit)
    }
  })
)

function strokeOf(pit) {
  if (pit.status === 'FULL') return '#ef5350'
  if (pit.nearFull) return '#f5a623'
  if (pit.status === 'OVERHAUL') return '#54637a'
  return '#2a3944'
}

function dotColorOf(pit) {
  if (pit.status === 'FULL') return '#ef5350'
  if (pit.nearFull) return '#f5a623'
  if (pit.status === 'OVERHAUL') return '#6b8290'
  return '#2ecc8f'
}

function dotClass(pit) {
  if (pit.status === 'FULL' || pit.nearFull) return 'danger'
  if (pit.status === 'OVERHAUL') return 'idle'
  return 'ok'
}

function barClass(pit) {
  if (pit.status === 'FULL' || pit.nearFull) return 'danger'
  return ''
}

function tagClass(pit) {
  if (pit.status === 'FULL' || pit.nearFull) return 'danger'
  if (pit.status === 'OVERHAUL') return 'primary'
  return 'ok'
}

function fmt(v) {
  return v === null || v === undefined ? '--' : Number(v).toFixed(2)
}

function num(v, d = 1) {
  return v === null || v === undefined ? '--' : Number(v).toFixed(d)
}

function pct(rate) {
  return ((rate || 0) * 100).toFixed(1) + '%'
}

async function load() {
  try {
    const [r1, r2, r3] = await Promise.all([pitApi.list(), pitApi.dict(), disposalApi.frozenFurnaces()])
    pits.value = r1.data || []
    dict.value = r2.data || {}
    frozenFurnaces.value = r3.data || []
    if (!feed.pitNo && pits.value.length) feed.pitNo = pits.value[0].pitNo
    if (!unload.pitNo && pits.value.length) unload.pitNo = pits.value[1] ? pits.value[1].pitNo : pits.value[0].pitNo
    if (!feed.targetFurnaceCode && dict.value.furnaceCodes && dict.value.furnaceCodes.length) {
      feed.targetFurnaceCode = dict.value.furnaceCodes[0]
    }
    if (!feed.grabNo && dict.value.grabNos && dict.value.grabNos.length) feed.grabNo = dict.value.grabNos[0]
    selected.value = bays.value[0] || null
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openFeed(bay) {
  selected.value = bay
  feed.pitNo = bay.pitNo
  if (bay.grabNo) feed.grabNo = bay.grabNo
}

async function doFeed() {
  try {
    const res = await pitApi.feed(feed.pitNo, {
      grabNo: feed.grabNo,
      feedTon: feed.feedTon,
      targetFurnaceCode: feed.targetFurnaceCode
    })
    ElMessage.success(res.message || '投料成功')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doUnload() {
  try {
    const res = await pitApi.unload(unload.pitNo, {
      unloadTon: unload.unloadTon,
      truckNo: unload.truckNo,
      wasteCategory: unload.wasteCategory
    })
    ElMessage.success(res.message || '卸料成功')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doPump(pit) {
  try {
    const res = await pitApi.leachate(pit.pitNo, { pumpM3: 20 })
    ElMessage.success(res.message || '已登记')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doFerment(pit) {
  try {
    const days = (Number(pit.fermentationDays) || 0) + 0.5
    const res = await pitApi.ferment(pit.pitNo, days, Number(pit.pitTempC) || 40)
    ElMessage.success(res.message || '已更新')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.pit-svg {
  width: 100%;
  height: auto;
  background: linear-gradient(180deg, #101a21 0%, #0c1319 100%);
  border: 1px solid var(--wp-border);
  border-radius: 6px;
}

.bay {
  cursor: pointer;
}

.bay:hover polygon {
  opacity: 0.92;
}

.bay.selected polygon:first-of-type {
  stroke: var(--wp-accent);
  stroke-width: 2.4;
}

.svg-pitno {
  fill: #eaf6fc;
  font-size: 13px;
  font-weight: 700;
  text-anchor: middle;
}

.svg-inner {
  fill: #dbe6ec;
  font-size: 11px;
  text-anchor: middle;
}

.svg-stock {
  fill: #cfd8dc;
  font-size: 12px;
  font-weight: 600;
  text-anchor: middle;
}

.svg-rate {
  font-size: 11px;
  font-weight: 700;
  text-anchor: middle;
}

.svg-note,
.svg-note-small {
  fill: #6b8290;
  font-size: 11px;
  text-anchor: middle;
}

.svg-note {
  text-anchor: start;
}

.svg-leachate {
  fill: #9fdcf5;
  font-size: 8px;
  text-anchor: middle;
}

.pit-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.pit-chip {
  padding: 3px 9px;
  border-radius: 12px;
  border: 1px solid var(--wp-border);
  background: #131e26;
  font-size: 11px;
  color: var(--wp-text-dim);
  cursor: pointer;
}

.pit-chip:hover {
  border-color: var(--wp-accent);
  color: #eaf6fc;
}

.feed-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.wp-table td.right,
.wp-table th.right {
  text-align: right;
}

.wp-table td.strong {
  color: #eaf6fc;
  font-weight: 600;
}
</style>
