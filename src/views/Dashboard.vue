<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col v-for="s in statCards" :key="s.label" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ color: s.color, background: s.bg }">
              <el-icon :size="26"><component :is="s.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="stat-value">{{ s.value }}</div>
              <div class="stat-label">{{ s.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-bar"></span>
              <span>月度任务完成情况</span>
            </div>
          </template>
          <div ref="barRef" class="chart" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-bar"></span>
              <span>用户角色分布</span>
            </div>
          </template>
          <div ref="pieRef" class="chart" style="height:320px"></div>
        </el-card>
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
        { label: '用户总数', value: this.userCount, icon: 'UserFilled', color: '#409eff', bg: 'rgba(64,158,255,.12)' },
        { label: '待办任务', value: 18, icon: 'Clock', color: '#e6a23c', bg: 'rgba(230,162,60,.14)' },
        { label: '已完成任务', value: 421, icon: 'CircleCheck', color: '#67c23a', bg: 'rgba(103,194,58,.14)' },
        { label: '系统消息', value: 6, icon: 'Bell', color: '#9c5fe0', bg: 'rgba(156,95,224,.14)' }
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
    // 用户总数 / 角色分布都来自这里
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
        legend: { data: ['已完成', '待办'], top: 0 },
        grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
        xAxis: {
          type: 'category',
          data: ['1月', '2月', '3月', '4月', '5月', '6月'],
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          axisLabel: { color: '#909399' }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f0f2f5' } },
          axisLabel: { color: '#909399' }
        },
        series: [
          {
            name: '已完成',
            type: 'bar',
            barWidth: 14,
            itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
            data: [120, 200, 150, 80, 70, 110]
          },
          {
            name: '待办',
            type: 'bar',
            barWidth: 14,
            itemStyle: { color: '#e6a23c', borderRadius: [4, 4, 0, 0] },
            data: [20, 32, 18, 30, 25, 12]
          }
        ]
      })
    },

    initPieChart() {
      this.pieChart = echarts.init(this.$refs.pieRef)
      this.pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, icon: 'circle' },
        color: ['#409eff', '#67c23a'],
        series: [
          {
            name: '角色分布',
            type: 'pie',
            radius: ['48%', '68%'],
            center: ['50%', '45%'],
            itemStyle: { borderColor: '#fff', borderWidth: 3 },
            label: { formatter: '{b}\n{c} 人', color: '#606266' },
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
.stat-card {
  border: none;
  border-radius: 10px;
  margin-bottom: 16px;
}
.stat-card :deep(.el-card__body) { padding: 18px; }
.stat-inner { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2d3d;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.chart-row { margin-top: 4px; }
.panel {
  border: none;
  border-radius: 10px;
  margin-bottom: 16px;
}
.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1f2d3d;
}
.panel-bar {
  width: 4px;
  height: 15px;
  border-radius: 2px;
  background: linear-gradient(180deg, #409eff, #6ab0ff);
}
.chart { width: 100%; }
</style>
