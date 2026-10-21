<template>
  <div class="page forbidden-page">
    <section class="panel forbidden">
      <div class="code mono">403</div>
      <h2>页面暂时无法访问</h2>
      <p class="u-hint">当前账号没有该页面的权限，或权限接口暂时不可用。</p>
      <div class="actions">
        <el-button type="primary" @click="$router.push('/dashboard')">返回仪表盘</el-button>
        <el-button @click="refresh">刷新权限</el-button>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'AccessForbidden',
  methods: {
    async refresh() {
      try {
        await this.$store.dispatch('refreshAccess')
        this.$message.success('权限已刷新')
        this.$router.push('/dashboard')
      } catch (e) {
        this.$message.error(e.message || '权限刷新失败')
      }
    }
  }
}
</script>

<style scoped>
.forbidden-page { display: flex; align-items: center; justify-content: center; min-height: 60vh; }
.forbidden {
  width: min(520px, 100%);
  padding: 36px;
  text-align: center;
}
.code {
  font-family: var(--display);
  font-size: 64px;
  line-height: 1;
  letter-spacing: -.06em;
  color: var(--accent);
  text-shadow: 0 0 30px color-mix(in srgb, var(--accent) 35%, transparent);
}
.forbidden h2 { margin: 18px 0 8px; font-size: 22px; }
.actions { display: flex; justify-content: center; gap: 10px; margin-top: 24px; }
</style>
