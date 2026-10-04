<template>
  <div class="app-shell">
    <!-- ===== 顶部横向工艺流程图导航 ===== -->
    <header class="wp-header">
      <div class="header-top">
        <div class="brand">
          <span class="brand-mark">WP</span>
          <div class="brand-text">
            <div class="brand-title">生活垃圾焚烧发电厂 · 进厂与焚烧运行管理</div>
            <div class="brand-sub">WASTE-TO-ENERGY PLANT · 进厂计重 → 储坑发酵 → 焚烧炉 → 余热发电 → 烟气净化 → 灰渣渗滤</div>
          </div>
        </div>
        <div class="header-clock">
          <div class="clock-time mono">{{ clock }}</div>
          <div class="clock-date">{{ today }}</div>
        </div>
      </div>

      <div class="flow-nav">
        <div class="flow-track"></div>
        <div
          v-for="(node, index) in nodes"
          :key="node.key"
          class="flow-item"
        >
          <div
            class="flow-node"
            :class="{ active: activeKey === node.key }"
            @click="pickNode(node)"
          >
            <div class="node-icon" v-html="icons[node.icon]"></div>
            <div class="node-label">{{ node.label }}</div>
            <div class="node-index mono">{{ String(index + 1).padStart(2, '0') }}</div>
          </div>
          <div v-if="index < nodes.length - 1" class="flow-pipe">
            <span class="pipe-arrow"></span>
          </div>
        </div>
      </div>

      <!-- 当前节点的子功能项下拉 -->
      <div class="sub-nav">
        <div class="sub-current">
          <span class="wp-dot ok"></span>
          当前工序：<b>{{ activeNode.label }}</b>
        </div>
        <div class="sub-dropdown" @click="open = !open" v-click-outside="() => (open = false)">
          <span>子功能项</span>
          <span class="sub-count">{{ activeNode.subs.length }}</span>
          <span class="caret" :class="{ up: open }">▾</span>
          <div v-if="open" class="sub-menu">
            <div
              v-for="sub in activeNode.subs"
              :key="sub.hash"
              class="sub-menu-item"
              @click.stop="pickSub(sub)"
            >
              <span class="sub-menu-dot"></span>{{ sub.label }}
            </div>
          </div>
        </div>
        <div class="sub-chips">
          <span
            v-for="sub in activeNode.subs"
            :key="sub.hash"
            class="sub-chip"
            @click="pickSub(sub)"
          >{{ sub.label }}</span>
        </div>
      </div>
    </header>

    <main class="wp-main">
      <component :is="viewComp" />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import router from './router'
import IntakeView from './views/IntakeView.vue'
import PitView from './views/PitView.vue'
import FurnaceView from './views/FurnaceView.vue'
import EmissionView from './views/EmissionView.vue'
import DisposalView from './views/DisposalView.vue'

// main.js 固定不改（未调用 app.use(router)），这里直接驱动 router 实例做导航与地址同步，
// 页面切换由本地路径状态决定（不依赖 router 的内部注入）。
const VIEW_MAP = {
  '/intake': IntakeView,
  '/pit': PitView,
  '/furnace': FurnaceView,
  '/emission': EmissionView,
  '/disposal': DisposalView
}

function normalizePath(path) {
  if (!path || path === '/' || path === '/index.html') return '/intake'
  return path.replace(/\/+$/, '') || '/intake'
}

const currentPath = ref(normalizePath(window.location.pathname))
const route = computed(() => ({ path: currentPath.value }))
const viewComp = computed(() => VIEW_MAP[currentPath.value] || IntakeView)

router.afterEach((to) => {
  currentPath.value = normalizePath(to.path)
})

const nodes = [
  {
    key: 'intake', label: '卸料平台', icon: 'truck', path: '/intake',
    subs: [
      { label: '当日地磅流水', hash: '#sec-flow' },
      { label: '当日进厂统计', hash: '#sec-stat' },
      { label: '来源区域汇总', hash: '#sec-source' }
    ]
  },
  {
    key: 'pit', label: '垃圾储坑', icon: 'pit', path: '/pit',
    subs: [
      { label: '储坑剖面', hash: '#sec-profile' },
      { label: '抓斗投料', hash: '#sec-feed' },
      { label: '渗滤液与堆酵', hash: '#sec-leachate' }
    ]
  },
  {
    key: 'furnace', label: '焚烧炉', icon: 'flame', path: '/furnace',
    subs: [
      { label: '炉型参数卡', hash: '#sec-cards' },
      { label: '炉型统计', hash: '#sec-typestat' },
      { label: '启停炉记录', hash: '#sec-logs' }
    ]
  },
  {
    key: 'disposal', label: '异常工况处置', icon: 'shield', path: '/disposal',
    subs: [
      { label: '处置单列表', hash: '#sec-disposal-list' },
      { label: '处置详情与证据轨迹', hash: '#sec-disposal-detail' },
      { label: '恢复条件与投料联锁', hash: '#sec-condition' }
    ]
  },
  {
    key: 'boiler', label: '余热锅炉', icon: 'steam', path: '/furnace',
    subs: [
      { label: '锅炉蒸汽工况', hash: '#sec-boiler' },
      { label: '月度处理量与发电量', hash: '#sec-monthly' }
    ]
  },
  {
    key: 'turbine', label: '汽轮发电', icon: 'bolt', path: '/furnace',
    subs: [
      { label: '机组参数', hash: '#sec-turbine' },
      { label: '发电量登记', hash: '#sec-generation' }
    ]
  },
  {
    key: 'clean', label: '烟气净化', icon: 'filter', path: '/emission',
    subs: [
      { label: '排放仪表盘', hash: '#sec-gauges' },
      { label: '超标告警', hash: '#sec-alarms' },
      { label: '超标次数排行', hash: '#sec-rank' }
    ]
  },
  {
    key: 'ash', label: '灰渣渗滤', icon: 'ash', path: '/emission',
    subs: [
      { label: '灰渣与渗滤液台账', hash: '#sec-ash' },
      { label: '当日产出登记', hash: '#sec-ash-form' }
    ]
  }
]

const icons = {
  truck: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="1" y="7" width="13" height="9" rx="1"/><path d="M14 11h4l3 3v2h-7z"/><circle cx="5.5" cy="18" r="1.8"/><circle cx="17.5" cy="18" r="1.8"/></svg>',
  pit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M3 5h18l-2.5 14H5.5z"/><path d="M6 14h12"/><path d="M9 5v9M15 5v9"/></svg>',
  flame: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 2c4 5 6 7 6 11a6 6 0 0 1-12 0c0-3 2-4 3-6 1 2 2 2 2 0 0-2 1-3 1-5z"/></svg>',
  shield: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 2 4 5v6c0 5 3.5 9 8 11 4.5-2 8-6 8-11V5z"/><path d="M8.5 12l2.5 2.5L16 9.5"/></svg>',
  steam: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="4" y="11" width="16" height="10" rx="1.5"/><path d="M8 8c0-2 2-2 2-4M12 8c0-2 2-2 2-4M16 8c0-2 2-2 2-4"/></svg>',
  bolt: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M13 2 4 14h7l-1 8 9-12h-7z"/></svg>',
  filter: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M3 4h18l-7 8v8l-4-2v-6z"/></svg>',
  ash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="9" width="18" height="11" rx="1.5"/><path d="M3 13h18M9 9V4h6v5"/></svg>'
}

const activeKey = ref(nodes[0].key)
const open = ref(false)

const activeNode = computed(() => nodes.find((n) => n.key === activeKey.value) || nodes[0])

watch(
  () => route.value.path,
  (path) => {
    // 路由变化时，若当前节点不属于该路由，则切到该路由下的第一个节点
    if (activeNode.value.path !== path) {
      const first = nodes.find((n) => n.path === path)
      if (first) activeKey.value = first.key
    }
  },
  { immediate: true }
)

function pickNode(node) {
  activeKey.value = node.key
  open.value = false
  currentPath.value = node.path
  router.push(node.path).catch(() => {})
}

function pickSub(sub) {
  open.value = false
  router.push({ path: activeNode.value.path, hash: sub.hash }).catch(() => {})
  setTimeout(() => {
    const el = document.querySelector(sub.hash)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, 120)
}

const clock = ref('')
const today = ref('')
let timer = null

function tick() {
  const now = new Date()
  clock.value = now.toTimeString().slice(0, 8)
  today.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'long' })
}

function syncFromLocation() {
  currentPath.value = normalizePath(window.location.pathname)
  return router.replace(window.location.pathname + window.location.search + window.location.hash)
}

onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
  // 首次导航（等价于 app.use(router) 时做的那次初始化 push）
  syncFromLocation().catch(() => {})
  window.addEventListener('popstate', syncFromLocation)
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('popstate', syncFromLocation)
})
</script>

<script>
export default {
  directives: {
    'click-outside': {
      mounted(el, binding) {
        el.__outside__ = (e) => {
          if (!el.contains(e.target)) binding.value()
        }
        document.addEventListener('click', el.__outside__)
      },
      unmounted(el) {
        document.removeEventListener('click', el.__outside__)
      }
    }
  }
}
</script>

<style scoped>
.app-shell {
  min-height: 100%;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(41, 182, 246, 0.06), transparent 60%),
    var(--wp-bg);
}

/* ===== 顶部 ===== */
.wp-header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 10px 18px 0;
  background: linear-gradient(180deg, #101a21 0%, #0d151b 100%);
  border-bottom: 1px solid var(--wp-border);
  box-shadow: 0 6px 22px rgba(0, 0, 0, 0.45);
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--wp-primary) 0%, #263238 100%);
  border: 1px solid var(--wp-border-2);
  color: #cfd8dc;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #e7f1f6;
}

.brand-sub {
  margin-top: 2px;
  font-size: 11px;
  color: var(--wp-text-mute);
  letter-spacing: 0.5px;
}

.header-clock {
  text-align: right;
}

.clock-time {
  font-size: 20px;
  font-weight: 700;
  color: var(--wp-accent);
  letter-spacing: 2px;
}

.clock-date {
  font-size: 11px;
  color: var(--wp-text-mute);
}

/* ===== 工艺流程链 ===== */
.flow-nav {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0;
  margin: 12px 0 6px;
  padding: 6px 0;
  overflow-x: auto;
}

.flow-track {
  position: absolute;
  left: 40px;
  right: 40px;
  top: 50%;
  height: 2px;
  background: repeating-linear-gradient(90deg, var(--wp-border-2) 0 8px, transparent 8px 14px);
}

.flow-item {
  display: flex;
  align-items: center;
}

.flow-node {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 26px 1fr;
  grid-template-rows: auto auto;
  align-items: center;
  gap: 0 8px;
  min-width: 118px;
  padding: 8px 12px;
  border-radius: 10px;
  background: linear-gradient(180deg, #1a2731 0%, #14202a 100%);
  border: 1px solid var(--wp-border);
  color: var(--wp-text-dim);
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.flow-node:hover {
  border-color: var(--wp-primary-3);
  color: var(--wp-text);
}

.flow-node.active {
  transform: translateY(-3px) scale(1.06);
  border-color: var(--wp-accent);
  background: linear-gradient(180deg, #22323e 0%, #17242e 100%);
  box-shadow: 0 0 0 1px rgba(41, 182, 246, 0.35), 0 8px 20px rgba(0, 0, 0, 0.5);
  color: #eaf6fc;
}

.node-icon {
  grid-row: span 2;
  width: 26px;
  height: 26px;
  color: var(--wp-primary-3);
}

.flow-node.active .node-icon {
  color: var(--wp-accent);
}

.node-icon svg {
  width: 100%;
  height: 100%;
}

.node-label {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  white-space: nowrap;
}

.node-index {
  font-size: 10px;
  color: var(--wp-text-mute);
  letter-spacing: 1px;
}

.flow-node.active .node-index {
  color: var(--wp-accent);
}

/* 带箭头的连线（管线流动动画） */
.flow-pipe {
  position: relative;
  z-index: 1;
  flex: 1 1 auto;
  min-width: 26px;
  height: 6px;
  margin: 0 -2px;
  border-radius: 3px;
  background-color: #16222b;
  background-image: linear-gradient(90deg, rgba(41, 182, 246, 0) 0%, rgba(41, 182, 246, 0.95) 50%, rgba(41, 182, 246, 0) 100%);
  background-size: 40px 100%;
  background-repeat: repeat-x;
  animation: wp-flow 1.4s linear infinite;
  border-top: 1px solid #22323d;
  border-bottom: 1px solid #22323d;
}

.pipe-arrow {
  position: absolute;
  right: -1px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 7px solid var(--wp-accent);
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
}

/* ===== 子功能项 ===== */
.sub-nav {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 2px 10px;
  border-top: 1px dashed var(--wp-border);
}

.sub-current {
  font-size: 12px;
  color: var(--wp-text-dim);
}

.sub-current b {
  color: #eaf6fc;
  letter-spacing: 1px;
}

.sub-dropdown {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--wp-border-2);
  border-radius: 6px;
  background: #16222b;
  color: var(--wp-text);
  font-size: 12px;
  cursor: pointer;
  user-select: none;
}

.sub-dropdown:hover {
  border-color: var(--wp-accent);
}

.sub-count {
  padding: 0 5px;
  border-radius: 8px;
  background: var(--wp-primary);
  color: #cfd8dc;
  font-size: 11px;
}

.caret {
  transition: transform 0.2s ease;
}

.caret.up {
  transform: rotate(180deg);
}

.sub-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 30;
  min-width: 190px;
  padding: 5px;
  background: var(--wp-panel-2);
  border: 1px solid var(--wp-border-2);
  border-radius: 6px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.55);
}

.sub-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: var(--wp-text);
}

.sub-menu-item:hover {
  background: rgba(41, 182, 246, 0.14);
}

.sub-menu-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--wp-accent);
}

.sub-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sub-chip {
  padding: 3px 9px;
  border-radius: 12px;
  border: 1px solid var(--wp-border);
  background: #131e26;
  color: var(--wp-text-dim);
  font-size: 11px;
  cursor: pointer;
  white-space: nowrap;
}

.sub-chip:hover {
  color: #eaf6fc;
  border-color: var(--wp-primary-3);
  background: rgba(55, 71, 79, 0.5);
}

/* ===== 内容区 ===== */
.wp-main {
  padding: 14px 18px 30px;
}
</style>
