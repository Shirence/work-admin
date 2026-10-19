<template>
  <div v-loading="loading" class="page">
    <header class="page-head">
      <div>
        <h2>概览</h2>
        <p class="u-hint">关键指标与趋势，数据来自后端接口</p>
      </div>
      <span class="u-label">Updated just now</span>
    </header>

    <div class="bento">
      <article
        v-for="(s, i) in statCards"
        :key="s.label"
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
          <h3>月度任务完成情况</h3>
          <span class="u-label">6 months</span>
        </div>
        <div ref="barRef" class="chart" style="height:300px"></div>
      </article>

      <article v-reveal="340" class="tile tile-chart col-4">
        <div class="tile-head">
          <h3>用户角色分布</h3>
          <span class="u-label">realtime</span>
        </div>
        <div ref="pieRef" class="chart" style="height:300px"></div>
      </article>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getUserList } from '../api/user'

export default {
  name: "Dashboard",
  data() {
    return {
      users: [],
      loading: false,
      barChart: null,
      pieChart: null
    }
  },
  computed: {
    userCount() {
      return this.users.length
    },
    roleStats() {
      let admin = 0
      let normal = 0
      this.users.forEach(u => {
        if (u.role === 'admin') admin++
        else normal++
      })
      return { admin, normal }
    },
    statCards() {
      return [
        { code: 'USR.TOTAL', label: '用户总数', value: this.userCount, icon: 'UserFilled', color: '#5ef2d6' },
        { code: 'TASK.PENDING', label: '待办任务', value: 18, icon: 'Clock', color: '#f4d35e' },
        { code: 'TASK.DONE', label: '已完成任务', value: 421, icon: 'CircleCheck', color: '#58a6ff' },
        { code: 'SYS.MSG', label: '系统消息', value: 6, icon: 'Bell', color: '#ff3d81' }
      ]
    }
  },
  created() {
    this.fetchUsers()
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
    async fetchUsers() {
      this.loading = true
      try {
        const res = await getUserList({})
        if (res.code === 200) {
          this.users = res.data || []
          this.updatePieChart()
        }
      } catch (e) {
        this.$message.error('获取统计数据失败')
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    initBarChart() {
      const p = this.chartPalette()
      this.barChart = echarts.init(this.$refs.barRef)
      this.barChart.setOption({
        tooltip: { trigger: 'axis', borderWidth: 1, borderColor: p.tooltipBorder, backgroundColor: p.tooltipBg, textStyle: { color: p.text, fontSize: 12 } },
        legend: { data: ['已完成', '待办'], top: 0, right: 0, icon: 'rect', itemWidth: 10, itemHeight: 10, textStyle: { color: p.muted, fontSize: 12 } },
        grid: { left: 8, right: 8, bottom: 4, top: 44, containLabel: true },
        xAxis: {
          type: 'category',
          data: ['1月', '2月', '3月', '4月', '5月', '6月'],
          axisLine: { lineStyle: { color: p.line } },
          axisTick: { show: false },
          axisLabel: { color: p.faint, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: p.split } },
          axisLabel: { color: p.faint, fontSize: 11 }
        },
        series: [
          {
            name: '已完成',
            type: 'bar',
            barWidth: 12,
            itemStyle: { color: p.bar1, borderRadius: [2, 2, 0, 0] },
            data: [120, 200, 150, 80, 70, 110]
          },
          {
            name: '待办',
            type: 'bar',
            barWidth: 12,
            itemStyle: { color: p.bar2, borderRadius: [2, 2, 0, 0] },
            data: [20, 32, 18, 30, 25, 12]
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
            data: []
          }
        ]
      })
      this.updatePieChart()
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
            bar2: '#ff3d81',
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
            bar2: '#d61f69',
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

    updatePieChart() {
      if (!this.pieChart) return
      this.pieChart.setOption({
        series: [
          {
            data: [
              { value: this.roleStats.admin, name: '管理员' },
              { value: this.roleStats.normal, name: '普通用户' }
            ]
          }
        ]
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
.page { display: flex; flex-direction: column; gap: 20px; }
.page-head { display: flex; align-items: flex-end; justify-content: space-between; }
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
  box-shadow: var(--shadow-md), 0 0 40px -24px rgba(94, 242, 214, .5);
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

@media (max-width: 1200px) {
  .tile-stat { grid-column: span 6; }
  .col-8, .col-4 { grid-column: span 12; }
}
@media (max-width: 680px) {
  .tile-stat { grid-column: span 12; }
}
</style>
