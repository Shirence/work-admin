<template>
  <router-view/>
</template>

<style>
/* =====================================================================
   设计系统 · 大留白白色基调（高级产品感）
   层级：材质感(CSS) - 呼吸感(动效) - 空间感(环境) - 组件系统
   ===================================================================== */
:root {
  --bg: #ffffff;
  --bg-sub: #fafbfc;
  --bg-inset: #f4f6f9;
  --surface: #ffffff;
  --surface-2: rgba(255, 255, 255, .72);

  --line: #eef1f4;
  --line-2: #e3e8ee;
  --line-3: #d5dce4;

  --text: #0d1520;
  --text-2: #5a6672;
  --text-3: #98a2ae;

  --accent: #0a84ff;
  --accent-2: #4da3ff;
  --accent-soft: rgba(10, 132, 255, .08);
  --accent-line: rgba(10, 132, 255, .28);

  --ok: #17a673;
  --warn: #d99100;
  --danger: #e5484d;
  --violet: #7c5cff;

  --r-sm: 6px;
  --r-md: 10px;
  --r-lg: 14px;

  --shadow-xs: 0 1px 2px rgba(13, 21, 32, .04);
  --shadow-sm: 0 1px 2px rgba(13, 21, 32, .04), 0 2px 6px -1px rgba(13, 21, 32, .06);
  --shadow-md: 0 2px 4px rgba(13, 21, 32, .04), 0 10px 24px -6px rgba(13, 21, 32, .10);
  --shadow-lg: 0 4px 8px rgba(13, 21, 32, .04), 0 24px 48px -12px rgba(13, 21, 32, .16);

  --ease: cubic-bezier(.16, 1, .3, 1);
  --ease-2: cubic-bezier(.4, 0, .2, 1);
  --dur-1: .18s;
  --dur-2: .32s;
  --dur-3: .6s;

  --mono: 'JetBrains Mono', 'Cascadia Mono', Consolas, 'SF Mono', Menlo, monospace;
  --sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;

  --el-color-primary: #0a84ff;
  --el-color-primary-light-3: #4da3ff;
  --el-color-primary-light-5: #80bbff;
  --el-color-primary-light-7: #b3d4ff;
  --el-color-primary-light-8: #cce2ff;
  --el-color-primary-light-9: #eaf3ff;
  --el-color-primary-dark-2: #0069d9;
  --el-color-success: #17a673;
  --el-color-warning: #d99100;
  --el-color-danger: #e5484d;
  --el-border-radius-base: 8px;
  --el-border-radius-small: 6px;
  --el-border-color: #e3e8ee;
  --el-border-color-light: #eef1f4;
  --el-text-color-primary: #0d1520;
  --el-text-color-regular: #5a6672;
  --el-font-family: var(--sans);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { height: 100%; }
body {
  font-family: var(--sans);
  color: var(--text);
  background: var(--bg-sub);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

/* ============ 材质感 ============ */
.noise::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: .5;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.045'/%3E%3C/svg%3E");
}

.mesh {
  background-color: var(--bg-sub);
  background-image:
    radial-gradient(900px 500px at 12% -8%, rgba(10, 132, 255, .10), transparent 60%),
    radial-gradient(700px 420px at 92% 4%, rgba(124, 92, 255, .07), transparent 62%),
    radial-gradient(800px 500px at 60% 108%, rgba(23, 166, 115, .06), transparent 60%);
}

.glass {
  background: var(--surface-2);
  backdrop-filter: saturate(180%) blur(14px);
  -webkit-backdrop-filter: saturate(180%) blur(14px);
}

.surface {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
}

/* ============ 呼吸感 ============ */
.reveal {
  opacity: 0;
  transform: translateY(14px);
  transition: opacity var(--dur-3) var(--ease), transform var(--dur-3) var(--ease);
  will-change: opacity, transform;
}
.reveal.is-visible { opacity: 1; transform: none; }

@media (prefers-reduced-motion: reduce) {
  .reveal { opacity: 1 !important; transform: none !important; transition: none !important; }
  * { animation-duration: .01ms !important; transition-duration: .01ms !important; }
}

/* ============ 工具类 ============ */
.mono { font-family: var(--mono); }

.u-label {
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: .12em;
  text-transform: uppercase;
  color: var(--text-3);
}

.u-hint { font-size: 12px; color: var(--text-3); }

::-webkit-scrollbar { width: 10px; height: 10px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: #dfe5ec; border-radius: 999px; border: 3px solid transparent; background-clip: content-box; }
::-webkit-scrollbar-thumb:hover { background: #c6d0da; background-clip: content-box; }
</style>
