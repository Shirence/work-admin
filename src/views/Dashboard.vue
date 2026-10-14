<template>
  <div v-loading="loading">
    <el-card>
      <h3>仪表盘首页</h3>
      <p>欢迎进入工作中台，展示统计数据</p>

      <el-row :gutter="20" style="margin-top:20px">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="num-card">用户总数：{{ userCount }}</div>
            <div class="num-tip">来自 getUserList() 接口</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="num-card">待办任务：18</div>
            <div class="num-tip">静态模拟数据</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="num-card">已完成任务：421</div>
            <div class="num-tip">静态模拟数据</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="num-card">系统消息：6</div>
            <div class="num-tip">静态模拟数据</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>月度任务完成情况（静态模拟数据）</template>
          <div ref="barRef" class="chart" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>用户角色分布（实时）</template>
          <div ref="pieRef" class="chart" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
// 第 4 课封装好的接口，直接复用
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
    // 用户总数直接由接口数据算出来
    userCount() {
      return this.users.length
    },
    // 角色分布：admin 算管理员，其余算普通用户
    roleStats() {
      let admin = 0
      let normal = 0
      this.users.forEach(u => {
        if (u.role === 'admin') admin++
        else normal++
      })
      return { admin, normal }
    }
  },
  created() {
    this.fetchUsers()
  },
  mounted() {
    // DOM 渲染完成后再初始化图表
    this.initBarChart()
    this.initPieChart()   // 内部会调用 updatePieChart 填数据
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.barChart && this.barChart.dispose()
    this.pieChart && this.pieChart.dispose()
  },
  methods: {
    // 拉取用户列表（仪表盘的实时数据来源）
    async fetchUsers() {
      this.loading = true
      try {
        const res = await getUserList({})
        if (res.code === 200) {
          this.users = res.data || []
          // 数据到了就更新已有图表实例，不要重新 init
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
      // 月度任务暂时没有对应接口，保留静态演示数据
      this.barChart = echarts.init(this.$refs.barRef)
      this.barChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['已完成', '待办'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
        yAxis: { type: 'value' },
        series: [
          { name: '已完成', type: 'bar', data: [120, 200, 150, 80, 70, 110], itemStyle: { color: '#409EFF' } },
          { name: '待办', type: 'bar', data: [20, 32, 18, 30, 25, 12], itemStyle: { color: '#E6A23C' } }
        ]
      })
    },

    initPieChart() {
      this.pieChart = echarts.init(this.$refs.pieRef)
      this.pieChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [
          {
            name: '角色分布',
            type: 'pie',
            radius: ['45%', '65%'],
            center: ['50%', '45%'],
            data: []   // 真正的数据交给 updatePieChart
          }
        ]
      })
      this.updatePieChart()
    },

    // 用当前 roleStats 刷新饼图（数据变化时重复调用即可）
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
.num-card {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.num-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
.chart {
  width: 100%;
}
</style>
