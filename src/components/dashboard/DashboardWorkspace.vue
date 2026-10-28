<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { getDashboard } from '../../api/work'
import { dateTime } from '../work/workModel'
import DashboardMetrics from './DashboardMetrics.vue'
import TaskTrend from './TaskTrend.vue'
import MarkdownContent from '../common/MarkdownContent.vue'
const store = useStore(), router = useRouter(), data = ref(null), days = ref(7), loading = ref(false), error = ref(''), announcement = ref(null)
let serial = 0
const can = permission => store.getters.can(permission)
async function load() {
  const current = ++serial; loading.value = true; error.value = ''
  try {const result = await getDashboard(days.value); if(current === serial) data.value = result}
  catch(failure) {
    if(current === serial) {
      const status = failure.response?.status
      error.value = status === 404
        ? '教师后端未提供 /api/dashboard（404），仪表盘暂无真实数据；请联系教师部署该接口。'
        : failure.userMessage || '统计加载失败，请重试'
    }
  }
  finally {if(current === serial) loading.value = false}
}
onMounted(load); onBeforeUnmount(() => ++serial)
</script>
<template>
  <section class="dashboard" v-loading="loading">
    <header class="dashboard-head"><div><h1>工作概览</h1><p v-if="data">数据更新于 {{ dateTime(data.generatedAt) }}</p></div><div class="dashboard-actions"><el-select v-model="days" :disabled="loading" aria-label="统计范围" @change="load"><el-option label="近 7 天" :value="7" /><el-option label="近 30 天" :value="30" /></el-select><el-button :loading="loading" @click="load">刷新数据</el-button></div></header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template v-if="data"><DashboardMetrics :overview="data.overview" />
      <div class="dashboard-grid"><el-card shadow="never"><template #header><strong>近 {{ days }} 日任务趋势</strong></template><TaskTrend :rows="data.trend" /></el-card><el-card shadow="never"><template #header><strong>团队角色分布</strong></template><div class="role-total">{{ data.overview.totalUsers }} <small>位团队成员</small></div><div v-for="role in data.roles" :key="role.name" class="role-row"><div><span>{{ role.name }}</span><b>{{ role.count }}</b></div><el-progress :percentage="data.overview.totalUsers ? Math.round(role.count/data.overview.totalUsers*100) : 0" :show-text="false" :stroke-width="7" /></div><p class="dashboard-note">{{ data.overview.onlineUsers }} 人拥有有效登录会话</p></el-card></div>
      <div class="dashboard-grid"><el-card shadow="never"><template #header><div class="panel-title"><strong>最近任务</strong><el-button v-if="can('task:read')" link type="primary" @click="router.push('/tasks')">查看全部 →</el-button></div></template><el-table :data="data.tasks" empty-text="暂无任务，从新建任务开始" stripe><el-table-column prop="title" label="任务" min-width="190" show-overflow-tooltip /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'done' ? 'success' : 'info'">{{ {todo:'待开始',doing:'进行中',done:'已完成'}[row.status] }}</el-tag></template></el-table-column><el-table-column prop="dueDate" label="截止日期" width="120" /></el-table></el-card><el-card shadow="never"><template #header><div class="panel-title"><strong>团队公告</strong><el-button v-if="can('announcement:read')" link type="primary" @click="router.push('/announcements')">查看全部 →</el-button></div></template><button v-for="item in data.announcements" :key="item.id" class="announcement-row" @click="announcement = item"><span><el-tag v-if="item.important" size="small" type="danger">重要</el-tag>{{ item.title }}</span><small>{{ dateTime(item.updatedAt) }}</small></button><el-empty v-if="!data.announcements.length" description="暂无已发布公告" :image-size="55" /></el-card></div>
      <div class="dashboard-grid"><el-card shadow="never"><template #header><strong>部门任务分布</strong></template><div v-for="department in data.departments" :key="department.name" class="department-row"><div><span>{{ department.name }}</span><small>完成 {{ department.completed }} / {{ department.total }}</small></div><el-progress :percentage="Math.round(department.completed/department.total*100)" /></div><el-empty v-if="!data.departments.length" description="创建任务后显示部门统计" :image-size="55" /></el-card><el-card shadow="never"><template #header><strong>最近动态</strong></template><el-timeline v-if="data.activities.length"><el-timeline-item v-for="item in data.activities" :key="item.id" :timestamp="dateTime(item.createdAt)"><b>{{ item.actorName }}</b> {{ item.action }}</el-timeline-item></el-timeline><el-empty v-else description="暂无工作动态" :image-size="55" /></el-card></div>
      <div class="quick-actions"><el-button v-if="can('task:create')" type="primary" @click="router.push('/tasks')">管理任务</el-button><el-button v-if="can('report:access')" @click="router.push('/reports')">填写周报</el-button><el-button v-if="can('notification:access')" @click="router.push('/notifications')">查看通知</el-button><el-button v-if="can('message:access')" @click="router.push('/messages')">打开 AI 助手</el-button></div>
    </template><el-empty v-else-if="error" description="数据暂不可用，请点击刷新重试" />
    <el-dialog :model-value="!!announcement" :title="announcement?.title" width="min(750px,94vw)" @update:model-value="!$event && (announcement = null)"><MarkdownContent :content="announcement?.content || ''" /></el-dialog>
  </section>
</template>
<style scoped>
.dashboard { max-width:1600px; margin:0 auto; }.dashboard-head { display:flex; justify-content:space-between; align-items:center; gap:18px; margin:6px 0 26px; }.eyebrow { font-size: 12px; font-weight:650; color:#7d97bc; letter-spacing:2px; }h1 { margin:9px 0; font-size:25px; letter-spacing:1px; }.dashboard-head p,.dashboard-note { font-size:12px; color:#8b9aae; line-height:1.8; }.dashboard-actions { display:flex; gap:10px; flex-shrink:0; }.dashboard-actions .el-select { width:120px; }.dashboard-grid { display:grid; grid-template-columns:minmax(0,1.6fr) minmax(0,1fr); gap:18px; margin-top:20px; }.dashboard-grid .el-card { border-radius:14px; border-color:#e4ebf5; border-width:0.5px; min-width:0; }strong { font-size:14px; font-weight:600; }.role-total { font-size:32px; font-weight:650; margin:5px 0 22px; }.role-total small { font-size:12px; font-weight:400; color:#91a0b4; }.role-row,.department-row { margin-bottom:20px; }.role-row>div:first-child,.department-row>div:first-child { display:flex; justify-content:space-between; margin-bottom:9px; font-size:12px; }.department-row small { color:#8c9bb0; }.panel-title { display:flex; justify-content:space-between; align-items:center; }.announcement-row { display:block; width:100%; border:0; background:none; text-align:left; padding:12px 0; border-bottom:1px solid #edf1f7; font:inherit; cursor:pointer; color:var(--app-text); }.announcement-row span { display:flex; align-items:center; gap:8px; font-size:13px; }.announcement-row small { display:block; margin-top:8px; font-size: 12px; color:#97a4b5; }.quick-actions { display:flex; flex-wrap:wrap; gap:10px; margin:22px 0; }.quick-actions .el-button { margin:0; }.el-alert { margin-bottom:18px; }@media(max-width:1000px) { .dashboard-grid { grid-template-columns:minmax(0,1fr); } }@media(max-width:600px) { .dashboard-head { align-items:flex-start; flex-direction:column; gap:12px; }.dashboard-head h1 { font-size:22px; } }
</style>
