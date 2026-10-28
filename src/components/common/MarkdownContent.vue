<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '../../utils/markdown'

const props = defineProps({ content: { type: String, required: true } })
const rendered = computed(() => renderMarkdown(props.content))
async function copyCode(event) {
  const button = event.target.closest?.('button.md-copy')
  if (!button || !event.currentTarget.contains(button)) return
  const code = button.closest('.md-code-block')?.querySelector('pre code')
  if (!code) return
  try {
    await navigator.clipboard.writeText(code.textContent)
    ElMessage.success('代码已复制')
  } catch { ElMessage.warning('复制失败，请选中代码手动复制') }
}
</script>

<template>
  <!-- 仅渲染关闭原始 HTML、转义代码后的解析结果，禁止直接绑定原始回答。 -->
  <div class="markdown-content" @click="copyCode" v-html="rendered" />
</template>

<style scoped>
.markdown-content { min-width:0; color:#2c3e4d; font-size:14px; line-height:1.85; overflow-wrap:anywhere; }
.markdown-content :deep(> :first-child) { margin-top:0; }
.markdown-content :deep(> :last-child) { margin-bottom:0; }
.markdown-content :deep(h1), .markdown-content :deep(h2), .markdown-content :deep(h3), .markdown-content :deep(h4), .markdown-content :deep(h5), .markdown-content :deep(h6) { color:#243746; line-height:1.45; font-weight:650; margin:28px 0 12px; }
.markdown-content :deep(h1) { font-size:25px; padding-bottom:12px; border-bottom:1px solid #e4e9ed; }
.markdown-content :deep(h2) { font-size:21px; padding-bottom:9px; border-bottom:1px solid #e8edf0; }
.markdown-content :deep(h3) { font-size:18px; }
.markdown-content :deep(h4), .markdown-content :deep(h5), .markdown-content :deep(h6) { font-size:15px; }
.markdown-content :deep(p) { margin:12px 0; }
.markdown-content :deep(strong) { color:#233c4a; font-weight:650; }
.markdown-content :deep(ul), .markdown-content :deep(ol) { padding-left:1.8em; margin:12px 0; }
.markdown-content :deep(li) { padding-left:3px; margin:5px 0; }
.markdown-content :deep(li::marker) { color:#577368; }
.markdown-content :deep(li > p) { margin:6px 0; }
.markdown-content :deep(li > ul), .markdown-content :deep(li > ol) { margin:5px 0; }
.markdown-content :deep(blockquote) { margin:18px 0; padding:3px 18px; border-left:3px solid #5ca58b; border-radius:0 7px 7px 0; color:#587166; background:#edf5f1; }
.markdown-content :deep(blockquote p) { margin:8px 0; }
.markdown-content :deep(a) { color:#168365; text-decoration:none; text-underline-offset:3px; }
.markdown-content :deep(a:hover) { text-decoration:underline; }
.markdown-content :deep(a:focus-visible), .markdown-content :deep(button:focus-visible), .markdown-content :deep([tabindex]:focus-visible) { outline:2px solid #389777; outline-offset:3px; }
.markdown-content :deep(hr) { border:0; border-top:1px solid #e2e8ed; margin:24px 0; }
.markdown-content :deep(:not(pre) > code) { font-family:'SFMono-Regular',Consolas,'Liberation Mono',monospace; font-size:.88em; background:#ecf0f3; color:#be5470; border:1px solid #e4e9ed; border-radius:4px; padding:2px 5px; }
.markdown-content :deep(.md-table-scroll) { max-width:100%; overflow-x:auto; margin:18px 0; border:1px solid #dfe6ec; border-radius:9px; scrollbar-width:thin; }
.markdown-content :deep(table) { width:100%; border-spacing:0; border-collapse:separate; font-size:13px; line-height:1.7; }
.markdown-content :deep(th), .markdown-content :deep(td) { padding:11px 15px; text-align:left; min-width:100px; border-bottom:1px solid #e5ebef; vertical-align:top; }
.markdown-content :deep(th) { background:#edf3f5; color:#3b5361; font-weight:600; white-space:nowrap; }
.markdown-content :deep(td + td), .markdown-content :deep(th + th) { border-left:1px solid #e5ebef; }
.markdown-content :deep(tbody tr:nth-child(even)) { background:#f3f6f8; }
.markdown-content :deep(tbody tr:last-child td) { border-bottom:0; }
.markdown-content :deep(.md-code-block) { min-width:0; max-width:100%; margin:18px 0; border:1px solid #263645; border-radius:10px; overflow:hidden; background:#192530; }
.markdown-content :deep(.md-code-toolbar) { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:9px 14px; background:#22323f; border-bottom:1px solid #30404d; color:#a8bdcd; font-size:12px; line-height:1.4; }
.markdown-content :deep(.md-copy) { flex-shrink:0; border:1px solid #4a5e6d; border-radius:5px; padding:4px 9px; color:#d6e3eb; background:transparent; cursor:pointer; font:inherit; font-size: 12px; }
.markdown-content :deep(.md-copy:hover) { border-color:#81baa8; color:#b8e3d1; background:#2b414d; }
.markdown-content :deep(pre) { max-width:100%; overflow:auto; margin:0; padding:17px 18px; scrollbar-width:thin; tab-size:2; }
.markdown-content :deep(pre code) { white-space:pre; overflow-wrap:normal; word-break:normal; font-family:'SFMono-Regular',Consolas,'Liberation Mono',monospace; font-size:12.5px; line-height:1.75; color:#d8e3ed; }
.markdown-content :deep(.hljs-comment), .markdown-content :deep(.hljs-quote) { color:#869caf; }
.markdown-content :deep(.hljs-keyword), .markdown-content :deep(.hljs-selector-tag), .markdown-content :deep(.hljs-literal) { color:#c7a6ed; }
.markdown-content :deep(.hljs-string), .markdown-content :deep(.hljs-regexp), .markdown-content :deep(.hljs-addition), .markdown-content :deep(.hljs-attribute) { color:#a9d4ae; }
.markdown-content :deep(.hljs-number), .markdown-content :deep(.hljs-symbol), .markdown-content :deep(.hljs-meta), .markdown-content :deep(.hljs-deletion) { color:#efb78c; }
.markdown-content :deep(.hljs-title), .markdown-content :deep(.hljs-name), .markdown-content :deep(.hljs-section), .markdown-content :deep(.hljs-type) { color:#9dc6ff; }
.markdown-content :deep(.hljs-attr), .markdown-content :deep(.hljs-property), .markdown-content :deep(.hljs-built_in) { color:#edd497; }
@media(max-width:700px) { .markdown-content :deep(h1) { font-size:22px; }.markdown-content :deep(h2) { font-size:19px; }.markdown-content :deep(th), .markdown-content :deep(td) { padding:9px 11px; }.markdown-content :deep(pre) { padding:14px; } }
</style>
