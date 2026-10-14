<!-- 第 6.5 节可选练习：把搜索栏也封装成组件 -->
<template>
  <div class="search-box">
    <el-input
      :model-value="modelValue"
      :placeholder="placeholder"
      style="width:220px"
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('search')"
    />
    <el-button type="primary" @click="$emit('search')">查询</el-button>
    <el-button type="success" @click="$emit('add')">新增</el-button>
  </div>
</template>

<script>
export default {
  name: "SearchBar",
  props: {
    modelValue: { type: String, default: "" },
    placeholder: { type: String, default: "请输入关键字" }
  },
  emits: ["update:modelValue", "search", "add"]
}
</script>

<style scoped>
.search-box {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
