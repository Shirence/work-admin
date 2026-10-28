<script setup>
import { useRoute } from 'vue-router'
import WorkManagement from '../components/work/WorkManagement.vue'
const route = useRoute()
</script>
<template><WorkManagement :key="route.meta.resource" :resource="route.meta.resource" /></template>
