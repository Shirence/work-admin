<script setup>
import { computed, ref } from 'vue'
const props = defineProps({ rows: { type: Array, required: true } })
const hover = ref(null)
const max = computed(() => Math.max(1, ...props.rows.flatMap(row => [row.created, row.completed])))
const points = computed(() => props.rows.map((row,index) => ({ ...row, x: 45 + index * 610 / Math.max(1,props.rows.length - 1), y: 205 - row.created / max.value * 155 })))
const line = computed(() => points.value.map((p,index) => `${index ? 'L' : 'M'} ${p.x} ${p.y}`).join(' '))
</script>
<template>
  <div class="trend-summary"><span><i class="created" /> 新建任务</span><span><i class="completed" /> 已完成任务（按最近更新时间）</span><small v-if="hover">{{ hover.date }} / 新建 {{ hover.created }} / 完成 {{ hover.completed }}</small></div>
  <svg viewBox="0 0 700 255" role="img" aria-label="任务新建与完成趋势" @mouseleave="hover = null">
    <g v-for="i in 5" :key="i"><line x1="35" x2="675" :y1="205-(i-1)*39" :y2="205-(i-1)*39" stroke="#edf1f7" /><text x="22" :y="209-(i-1)*39" text-anchor="end">{{ Math.round(max*(i-1)/4) }}</text></g>
    <path :d="line" fill="none" stroke="#5083eb" stroke-width="3" stroke-linecap="round" />
    <g v-for="(p,index) in points" :key="p.date" @mouseenter="hover = p"><rect :x="p.x-6" :y="205-p.completed/max*155" width="12" :height="p.completed/max*155" rx="3" fill="#af9eea" /><circle :cx="p.x" :cy="p.y" r="4" fill="white" stroke="#5083eb" stroke-width="2" /><text v-if="rows.length <= 7 || index % 5 === 0 || index === rows.length-1" :x="p.x" y="236" text-anchor="middle">{{ p.date.slice(5) }}</text><rect :x="p.x-9" y="30" width="18" height="180" fill="transparent"><title>{{ p.date }}：新建 {{ p.created }}，完成 {{ p.completed }}</title></rect></g>
  </svg>
</template>
<style scoped>
svg { width:100%; display:block; min-height:180px; }text { font-size: 12px; fill:#8d9baf; }.trend-summary { display:flex; flex-wrap:wrap; gap:16px; color:#8b99ad; font-size: 12px; margin-bottom:15px; }.trend-summary span { display:flex; align-items:center; gap:6px; }i { width:9px; height:9px; border-radius:50%; }.created { background:#5083eb; }.completed { background:#af9eea; }
</style>
