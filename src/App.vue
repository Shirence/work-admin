<template>
  <router-view/>
</template>

<style>
/* =================================================================
   未来科技终端 · 白色主题（Rhine Lab 风格）
   主色：科技青 #00a6e0   线条：#dce4ec   等宽字体标注
   ================================================================= */
:root {
  --bg: #ffffff;
  --bg-sub: #f4f7fa;
  --bg-deep: #eaeff4;
  --line: #dce4ec;
  --line-2: #c4d0db;
  --text: #101a24;
  --text-2: #56687a;
  --text-3: #93a1af;
  --accent: #00a6e0;
  --accent-2: #22c8e8;
  --accent-soft: rgba(0, 166, 224, .08);
  --ok: #26b36b;
  --warn: #f0a020;
  --danger: #e04b4b;
  --mono: 'JetBrains Mono', 'Cascadia Mono', Consolas, 'SF Mono', Menlo, 'DejaVu Sans Mono', monospace;

  /* 覆盖 Element Plus 主题 */
  --el-color-primary: #00a6e0;
  --el-color-primary-light-3: #4dc0e8;
  --el-color-primary-light-5: #80d1ee;
  --el-color-primary-light-7: #b3e2f5;
  --el-color-primary-light-8: #cceafa;
  --el-color-primary-light-9: #eaf6fd;
  --el-color-primary-dark-2: #0085b3;
  --el-border-radius-base: 3px;
  --el-border-radius-small: 2px;
  --el-text-color-primary: #101a24;
  --el-text-color-regular: #47586a;
  --el-border-color: #dce4ec;
  --el-border-color-light: #e6ecf2;
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { height: 100%; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', 'PingFang SC', sans-serif;
  color: var(--text);
  background: var(--bg-sub);
  -webkit-font-smoothing: antialiased;
}

/* ---- 工具类 ---- */
.mono { font-family: var(--mono); }

/* 科技网格背景 */
.tech-grid {
  background-image:
    linear-gradient(rgba(0, 166, 224, .055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 166, 224, .055) 1px, transparent 1px);
  background-size: 26px 26px;
}

/* HUD 直角标记 */
.hud { position: relative; }
.hud::before,
.hud::after {
  content: '';
  position: absolute;
  width: 11px;
  height: 11px;
  border: 0 solid var(--accent);
  pointer-events: none;
  z-index: 1;
}
.hud::before { top: 0; left: 0; border-top-width: 2px; border-left-width: 2px; }
.hud::after { bottom: 0; right: 0; border-bottom-width: 2px; border-right-width: 2px; }

/* 闪烁光标 */
@keyframes cursor-blink { 0%, 49% { opacity: 1; } 50%, 100% { opacity: 0; } }
.cursor {
  display: inline-block;
  width: 7px;
  height: 1em;
  background: var(--accent);
  vertical-align: text-bottom;
  animation: cursor-blink 1s steps(1) infinite;
}

/* 扫描线 */
@keyframes scanline-move {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(2000%); }
}
.scanline {
  position: absolute;
  left: 0;
  right: 0;
  height: 34px;
  background: linear-gradient(180deg, transparent, rgba(0, 166, 224, .07), transparent);
  animation: scanline-move 5s linear infinite;
  pointer-events: none;
  overflow: hidden;
}

/* 滚动条 */
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #c9d4de; border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: #a9b8c6; }
</style>
