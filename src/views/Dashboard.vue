<template>
  <div v-loading="loading" class="page">
    <header class="page-head">
      <div>
        <h2>概览</h2>
        <p class="u-hint">真实统计数据，来自远程 MySQL 数据库</p>
      </div>
      <span class="u-label">{{ updatedLabel }}</span>
    </header>

    <div class="bento">
      <article
        v-for="(s, i) in statCards"
        :key="s.code"
        v-reveal="i * 70"
        class="tile tile-stat"
      >
        <div class="tile-top">
          <span class="u-label">{{ s.code }}</span>
          <el-icon class="tile-icon" :style="{ color: s.color }">
            <component :is="s.icon" />
          </el-icon>
        </div>
        <div class="tile-value">{{ s.value }}</div>
        <div class="tile-label">{{ s.label }}</div>
        <span class="tile-accent" :style="{ background: s.color }"></span>
      </article>

      <article v-reveal="280" class="tile tile-chart col-8">
        <div class="tile-head">
          <h3>近 6 日消息量</h3>
          <span class="u-label">MYSQL · REMOTE</span>
        </div>
        <div class="chart-wrap">
          <div ref="barRef" class="chart" style="height:300px"></div>
          <div v-if="!messageTotal" class="chart-empty chart-empty-text">近 6 日暂无消息</div>
        </div>
      </article>

      <article v-reveal="340" class="tile tile-chart col-4">
        <div class="tile-head">
          <h3>用户角色分布</h3>
          <span class="u-label">REALTIME</span>
        </div>
        <div class="chart-wrap">
          <div ref="pieRef" class="chart" style="height:300px"></div>
          <div v-if="!roleTotal" class="chart-empty">0</div>
        </div>
      </article>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getDashboardStats } from '../api/dashboard'

const EMPTY_STATS = {
  userCount: 0,
  accountCount: 0,
  conversationCount: 0,
  messageCount: 0,
  deviceCount: 0,
  activeDeviceCount: 0,
  dailyMessages: [],
  roleDistribution: [],
  fetchedAt: null
}

export default {
  name: 'Dashboard',
  data() {
    return {
      stats: { ...EMPTY_STATS },
      loading: false,
      barChart: null,
      pieChart: null
    }
  },
  computed: {
    roleTotal() {
      return (this.stats.roleDistribution || []).reduce((sum, r) => sum + (r.count || 0), 0)
    },
    messageTotal() {
      return (this.stats.dailyMessages || []).reduce((sum, d) => sum + (d.count || 0), 0)
    },
    statCards() {
      return [
        { code: 'USR.ACCOUNTS', label: '账号总数', value: this.stats.accountCount || 0, icon: 'UserFilled', color: 'var(--accent)' },
        { code: 'CHAT.ROOMS', label: '会话总数', value: this.stats.conversationCount || 0, icon: 'ChatDotRound', color: 'var(--ice)' },
        { code: 'CHAT.MSG', label: '消息总数', value: this.stats.messageCount || 0, icon: 'Bell', color: 'var(--violet)' },
        { code: 'DEV.ACTIVE', label: '活跃设备', value: this.stats.activeDeviceCount || 0, icon: 'DataLine', color: 'var(--warn)' }
      ]
    },
    updatedLabel() {
      if (!this.stats.fetchedAt) return 'NO DATA'
      const date = new Date(this.stats.fetchedAt)
      if (Number.isNaN(date.getTime())) return 'NO DATA'
      return `UPDATED ${date.toLocaleTimeString('zh-CN', { hour12: false })}`
    }
  },
  created() {
    this.fetchAll()
  },
  mounted() {
    this.initBarChart()
    this.initPieChart()
    window.addEventListener('resize', this.handleResize)
    window.addEventListener('theme-change', this.handleThemeChange)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    window.removeEventListener('theme-change', this.handleThemeChange)
    this.barChart && this.barChart.dispose()
    this.pieChart && this.pieChart.dispose()
  },
  methods: {
    async fetchAll() {
      this.loading = true
      try {
        const statsRes = await getDashboardStats()
        this.stats = { ...EMPTY_STATS, ...(statsRes.data || {}) }
        this.updateBarChart()
        this.updatePieChart()
      } catch (e) {
        this.stats = { ...EMPTY_STATS }
        this.updateBarChart()
        this.updatePieChart()
        this.$message.error('获取统计数据失败')
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    initBarChart() {
      const p = this.chartPalette()
      const daily = this.stats.dailyMessages || []
      this.barChart = echarts.init(this.$refs.barRef)
      this.barChart.setOption({
        tooltip: { trigger: 'axis', borderWidth: 1, borderColor: p.tooltipBorder, backgroundColor: p.tooltipBg, textStyle: { color: p.text, fontSize: 12 } },
        grid: { left: 8, right: 8, bottom: 4, top: 24, containLabel: true },
        xAxis: {
          type: 'category',
          data: daily.map(d => d.date),
          axisLine: { lineStyle: { color: p.line } },
          axisTick: { show: false },
          axisLabel: { color: p.faint, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitLine: { lineStyle: { color: p.split } },
          axisLabel: { color: p.faint, fontSize: 11 }
        },
        series: [
          {
            name: '消息数',
            type: 'bar',
            barWidth: 14,
            itemStyle: { color: p.bar1, borderRadius: [2, 2, 0, 0] },
            data: daily.map(d => d.count)
          }
        ]
      })
    },

    initPieChart() {
      const p = this.chartPalette()
      this.pieChart = echarts.init(this.$refs.pieRef)
      this.pieChart.setOption({
        tooltip: { trigger: 'item', borderWidth: 1, borderColor: p.tooltipBorder, backgroundColor: p.tooltipBg, textStyle: { color: p.text, fontSize: 12 } },
        legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { color: p.muted, fontSize: 12 } },
        color: [p.bar1, p.pieSecond],
        series: [
          {
            name: '角色分布',
            type: 'pie',
            radius: ['56%', '76%'],
            center: ['50%', '44%'],
            itemStyle: { borderColor: p.pieBorder, borderWidth: 3 },
            label: { show: false },
            labelLine: { show: false },
            data: this.pieData()
          }
        ]
      })
    },

    pieData() {
      return (this.stats.roleDistribution || [])
        .filter(r => (r.count || 0) > 0)
        .map(r => ({ name: r.name || r.code, value: r.count }))
    },

    updateBarChart() {
      if (!this.barChart) return
      const daily = this.stats.dailyMessages || []
      this.barChart.setOption({
        xAxis: { data: daily.map(d => d.date) },
        series: [{ data: daily.map(d => d.count) }]
      })
    },

    updatePieChart() {
      if (!this.pieChart) return
      this.pieChart.setOption({ series: [{ data: this.pieData() }] })
    },

    chartPalette() {
      const dark = document.documentElement.classList.contains('dark')
      return dark
        ? {
            tooltipBg: 'rgba(5,10,16,.96)',
            tooltipBorder: 'rgba(94,242,214,.22)',
            text: '#e8f4f6',
            muted: '#9fb4be',
            faint: '#607680',
            line: 'rgba(126,196,208,.20)',
            split: 'rgba(126,196,208,.08)',
            bar1: '#5ef2d6',
            pieSecond: '#58a6ff',
            pieBorder: '#0a1018'
          }
        : {
            tooltipBg: 'rgba(255,255,255,.98)',
            tooltipBorder: 'rgba(0,168,143,.22)',
            text: '#081418',
            muted: '#415a63',
            faint: '#78909a',
            line: 'rgba(8,40,45,.16)',
            split: 'rgba(8,40,45,.07)',
            bar1: '#00a88f',
            pieSecond: '#0b6fd0',
            pieBorder: '#ffffff'
          }
    },

    handleThemeChange() {
      this.$nextTick(() => {
        if (this.barChart) { this.barChart.dispose(); this.barChart = null }
        if (this.pieChart) { this.pieChart.dispose(); this.pieChart = null }
        this.initBarChart()
        this.initPieChart()
      })
    },

    handleResize() {
      this.barChart && this.barChart.resize()
      this.pieChart && this.pieChart.resize()
    }
  }
}
</script>

<style scoped>
.page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }
.page-head h2 { font-size: 24px; font-weight: 700; letter-spacing: -.04em; margin-bottom: 4px; }

/* Bento 栅格 */
.bento {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 16px;
}
.tile {
  position: relative;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
  transition: transform var(--dur-2) var(--ease), box-shadow var(--dur-2) var(--ease), border-color var(--dur-2) var(--ease-2), background .35s var(--ease-2);
}
.tile::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--accent) 45%, transparent), transparent);
  opacity: .55;
  pointer-events: none;
}
.tile:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md), 0 0 40px -24px color-mix(in srgb, var(--accent) 55%, transparent);
  border-color: var(--line-2);
}

.tile-stat { grid-column: span 3; padding: 18px 18px 16px; overflow: hidden; }
.tile-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.tile-icon { font-size: 17px; opacity: .9; filter: drop-shadow(0 0 8px currentColor); }
.tile-value { font-family: var(--display); font-size: 34px; font-weight: 700; letter-spacing: -.04em; line-height: 1.05; }
.tile-label { font-size: 12px; color: var(--text-3); margin-top: 5px; }
.tile-accent {
  position: absolute;
  left: 0;
  top: 18px;
  bottom: 18px;
  width: 2px;
  border-radius: 0 2px 2px 0;
  opacity: .95;
  box-shadow: 0 0 14px currentColor;
}

.tile-chart { padding: 18px 18px 8px; }
.col-8 { grid-column: span 8; }
.col-4 { grid-column: span 4; }
.tile-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.tile-head h3 { font-family: var(--display); font-size: 15px; font-weight: 600; letter-spacing: -.02em; }
.chart { width: 100%; }
.chart-wrap { position: relative; }
.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: .12em;
  color: var(--text-3);
  text-transform: uppercase;
  pointer-events: none;
}
/* 中文长文案用正常字号与字距，避免 10px 等宽字看不清 */
.chart-empty-text {
  font-family: inherit;
  font-size: 12.5px;
  letter-spacing: .02em;
  color: var(--text-2);
}

@media (max-width: 1200px) {
  .tile-stat { grid-column: span 6; }
  .col-8, .col-4 { grid-column: span 12; }
}
@media (max-width: 680px) {
  .tile-stat { grid-column: span 12; }
}
</style>
