<template>
  <div v-loading="loading">
    <div class="sec-title mono">// OVERVIEW</div>

    <el-row :gutter="14">
      <el-col v-for="s in statCards" :key="s.label" :xs="12" :sm="12" :md="6">
        <div class="stat-card hud" :style="{ '--c': s.color }">
          <div class="stat-head">
            <span class="stat-code mono">{{ s.code }}</span>
            <el-icon class="stat-icon"><component :is="s.icon" /></el-icon>
          </div>
          <div class="stat-value mono">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-bar"></div>
        </div>
      </el-col>
    </el-row>

    <div class="sec-title mono">// ANALYTICS</div>

    <el-row :gutter="14">
      <el-col :xs="24" :md="14">
        <section class="panel hud">
          <header class="panel-head">
            <span class="panel-bar"></span>
            <span class="panel-title">月度任务完成情况</span>
            <span class="panel-tag mono">STATIC</span>
          </header>
          <div ref="barRef" class="chart" style="height:320px"></div>
        </section>
      </el-col>
      <el-col :xs="24" :md="10">
        <section class="panel hud">
          <header class="panel-head">
            <span class="panel-bar"></span>
            <span class="panel-title">用户角色分布</span>
            <span class="panel-tag mono">LIVE</span>
          </header>
          <div ref="pieRef" class="chart" style="height:320px"></div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getUserList } from '../api/user'

export default {
  name: "Dashboard",
  data() {
    return {
      users: [],        // 真实数据源：用户列表
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
        { code: 'USR.TOTAL', label: '用户总数', value: this.userCount, icon: 'UserFilled', color: '#00a6e0' },
        { code: 'TASK.PENDING', label: '待办任务', value: 18, icon: 'Clock', color: '#f0a020' },
        { code: 'TASK.DONE', label: '已完成任务', value: 421, icon: 'CircleCheck', color: '#26b36b' },
        { code: 'SYS.MSG', label: '系统消息', value: 6, icon: 'Bell', color: '#8b5cf6' }
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
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
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
      this.barChart = echarts.init(this.$refs.barRef)
      this.barChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['已完成', '待办'], top: 0, icon: 'rect' },
        grid: { left: '3%', right: '4%', bottom: '3%', top: 42, containLabel: true },
        xAxis: {
          type: 'category',
          data: ['1月', '2月', '3月', '4月', '5月', '6月'],
          axisLine: { lineStyle: { color: '#c4d0db' } },
          axisTick: { show: false },
          axisLabel: { color: '#93a1af', fontFamily: 'monospace', fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#eef2f6', type: 'dashed' } },
          axisLabel: { color: '#93a1af', fontFamily: 'monospace', fontSize: 11 }
        },
        series: [
          {
            name: '已完成',
            type: 'bar',
            barWidth: 12,
            itemStyle: { color: '#00a6e0' },
            data: [120, 200, 150, 80, 70, 110]
          },
          {
            name: '待办',
            type: 'bar',
            barWidth: 12,
            itemStyle: { color: '#f0a020' },
            data: [20, 32, 18, 30, 25, 12]
          }
        ]
      })
    },

    initPieChart() {
      this.pieChart = echarts.init(this.$refs.pieRef)
      this.pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, icon: 'rect', itemWidth: 10, itemHeight: 10 },
        color: ['#00a6e0', '#26b36b'],
        series: [
          {
            name: '角色分布',
            type: 'pie',
            radius: ['50%', '70%'],
            center: ['50%', '44%'],
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: { formatter: '{b}\n{c}', color: '#56687a', fontFamily: 'monospace', fontSize: 11 },
            labelLine: { lineStyle: { color: '#c4d0db' } },
            data: []
          }
        ]
      })
      this.updatePieChart()
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
.sec-title {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 1.5px;
  margin: 4px 0 10px;
}

/* ---------- 统计卡 ---------- */
.stat-card {
  background: var(--bg);
  border: 1px solid var(--line);
  padding: 16px 18px 14px;
  margin-bottom: 14px;
  position: relative;
  transition: all .18s;
}
.stat-card:hover {
  border-color: var(--c);
  box-shadow: 0 6px 20px rgba(16, 26, 36, .07);
}
.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.stat-code {
  font-size: 10px;
  letter-spacing: 1px;
  color: var(--text-3);
}
.stat-icon {
  font-size: 18px;
  color: var(--c);
  opacity: .85;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text);
  letter-spacing: 1px;
}
.stat-label {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 4px;
}
.stat-bar {
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: var(--c);
}

/* ---------- 图表面板 ---------- */
.panel {
  background: var(--bg);
  border: 1px solid var(--line);
  margin-bottom: 14px;
}
.panel-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
}
.panel-bar {
  width: 3px;
  height: 13px;
  background: var(--accent);
}
.panel-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}
.panel-tag {
  font-size: 9px;
  letter-spacing: 1px;
  color: var(--accent);
  border: 1px solid rgba(0, 166, 224, .35);
  background: var(--accent-soft);
  padding: 1px 6px;
}
.chart { width: 100%; }
</style>
