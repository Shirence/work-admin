<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listWork, markRead, markAllRead } from '../../api/work'
import { dateTime } from './workModel'
const router = useRouter(), items = ref([]), page = ref(1), total = ref(0), unread = ref(0), onlyUnread = ref(false), busy = ref(false), error = ref('')
async function load() {
  busy.value = true; error.value = ''
  try {const result = await listWork('notifications', { page: page.value, unread: onlyUnread.value }); items.value = result.items; total.value = result.total; unread.value = result.unread}
  catch (failure) {error.value = failure.userMessage || '加载失败，请重试'}
  finally {busy.value = false}
}
async function read(item) {
  if (busy.value) return
  busy.value = true
  try {await markRead(item.id); await router.push(item.link)} catch {} finally {busy.value = false}
}
async function all() {
  if (busy.value) return
  busy.value = true
  try {await markAllRead(); page.value = 1; await load()} catch {} finally {busy.value = false}
}
onMounted(load)
</script>
<template>
  <el-card shadow="never"><div class="notification-head"><div><h1 class="page-title">通知中心 <el-tag>{{ unread }} 条未读</el-tag></h1><p class="page-description">任务分配、进度变化与新公告会在这里通知你。</p></div><el-button :disabled="busy || !unread" @click="all">全部标记已读</el-button></div><div class="notification-tools"><el-checkbox v-model="onlyUnread" :disabled="busy" @change="page = 1; load()">只看未读</el-checkbox><el-button :disabled="busy" @click="load">刷新</el-button></div><el-alert v-if="error" type="error" :title="error" :closable="false" /><div v-loading="busy"><button v-for="item in items" :key="item.id" class="notification-item" :class="{ unread: !item.readAt }" :disabled="busy" @click="read(item)"><span><strong>{{ item.title }}</strong><time>{{ dateTime(item.createdAt) }}</time></span><p>{{ item.content }}</p><small>{{ item.readAt ? '已读' : '未读' }} / 查看详情 →</small></button><el-empty v-if="!items.length && !busy" description="暂无通知" /></div><el-pagination v-model:current-page="page" :page-size="20" :total="total" layout="prev, pager, next" :disabled="busy" @current-change="load" /></el-card>
</template>
<style scoped>
.notification-head,.notification-tools { display:flex; gap:16px; align-items:center; justify-content:space-between; }.notification-tools { margin:20px 0; justify-content:flex-start; }.notification-item { display:block; width:100%; text-align:left; padding:20px; border:1px solid #e7edf6; border-radius:10px; background:white; margin-bottom:12px; font:inherit; cursor:pointer; color:var(--app-text); }.notification-item.unread { border-left:3px solid var(--app-primary); background:#f8fbff; }.notification-item span { display:flex; justify-content:space-between; gap:10px; }.notification-item strong { font-size:14px; }.notification-item time,.notification-item small { color:#8291a4; font-size:12px; }.notification-item p { font-size:13px; margin:12px 0; line-height:1.7; white-space:pre-wrap; overflow-wrap:anywhere; }.el-pagination { margin-top:20px; }@media(max-width:600px) { .notification-head { align-items:flex-start; flex-wrap:wrap; }.notification-item span { flex-direction:column; } }
</style>
