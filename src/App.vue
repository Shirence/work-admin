<template>
  <router-view/>
</template>

<style>
/* =====================================================================
   设计系统 · DEEP-SPACE INDUSTRIAL HUD
   支持 light / dark 双主题，html.dark 触发深色
   主色：薄荷青 · 冰蓝 · 品红
   ===================================================================== */
:root {
  /* ---- light：冷白实验室 + 薄荷青 ---- */
  --bg: #f3f7f8;
  --bg-sub: #f6fafb;
  --bg-inset: #eaf1f3;
  --surface: #ffffff;
  --surface-2: rgba(255, 255, 255, .80);
  --panel: linear-gradient(160deg, rgba(255, 255, 255, .97), rgba(240, 247, 248, .97));

  --line: rgba(8, 40, 45, .10);
  --line-2: rgba(8, 40, 45, .18);
  --line-3: rgba(8, 40, 45, .28);

  --text: #081418;
  --text-2: #415a63;
  --text-3: #78909a;

  --accent: #00a88f;
  --accent-2: #3fd8bf;
  --accent-soft: rgba(0, 168, 143, .09);
  --accent-line: rgba(0, 168, 143, .28);
  --ice: #0b6fd0;
  --violet: #d61f69;
  --acid: #6d8500;
  --ok: #0a8f78;
  --warn: #a66a00;
  --danger: #d61f4c;

  --on-accent: #04211c;
  --on-remote: #ffffff;
  --auth-card: rgba(255, 255, 255, .84);
  --auth-border: rgba(0, 168, 143, .26);
  --remote-card: rgba(255, 255, 255, .86);
  --remote-border: rgba(214, 31, 105, .26);

  --grid-line: rgba(0, 168, 143, .14);
  --scan-line: rgba(8, 20, 24, .035);
  --scan-beam: rgba(0, 168, 143, .18);
  --glow-a: rgba(0, 168, 143, .22);
  --glow-b: rgba(11, 111, 208, .16);
  --glow-c: rgba(214, 31, 105, .10);
  --overlay: rgba(243, 247, 248, .72);

  --r-sm: 2px;
  --r-md: 4px;
  --r-lg: 8px;

  --shadow-xs: 0 1px 2px rgba(8, 20, 24, .04);
  --shadow-sm: 0 1px 2px rgba(8, 20, 24, .04), 0 8px 24px -12px rgba(8, 20, 24, .16);
  --shadow-md: 0 2px 4px rgba(8, 20, 24, .04), 0 18px 42px -18px rgba(8, 20, 24, .20);
  --shadow-lg: 0 4px 8px rgba(8, 20, 24, .04), 0 30px 80px -24px rgba(8, 20, 24, .24), 0 0 60px -30px rgba(0, 168, 143, .25);

  --ease: cubic-bezier(.16, 1, .3, 1);
  --ease-2: cubic-bezier(.4, 0, .2, 1);
  --ease-spring: cubic-bezier(.34, 1.56, .64, 1);
  --dur-1: .18s;
  --dur-2: .32s;
  --dur-3: .6s;

  --mono: 'JetBrains Mono', 'Cascadia Mono', Consolas, 'SF Mono', Menlo, monospace;
  --display: 'Syne', 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  --sans: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

  /* Element Plus light */
  --el-color-primary: #00a88f;
  --el-color-primary-light-3: #4fc7b4;
  --el-color-primary-light-5: #80d8c9;
  --el-color-primary-light-7: #b3e9df;
  --el-color-primary-light-8: #ccf1ea;
  --el-color-primary-light-9: #e6f8f4;
  --el-color-primary-dark-2: #007f6c;
  --el-color-success: #0a8f78;
  --el-color-warning: #a66a00;
  --el-color-danger: #d61f4c;
  --el-color-info: #6f8792;
  --el-bg-color: #ffffff;
  --el-bg-color-page: #f3f7f8;
  --el-bg-color-overlay: #ffffff;
  --el-text-color-primary: #081418;
  --el-text-color-regular: #415a63;
  --el-text-color-secondary: #78909a;
  --el-text-color-placeholder: #9aabb2;
  --el-border-color: rgba(8, 40, 45, .16);
  --el-border-color-light: rgba(8, 40, 45, .10);
  --el-border-color-lighter: rgba(8, 40, 45, .06);
  --el-fill-color-blank: #ffffff;
  --el-fill-color-light: #f0f6f7;
  --el-fill-color: #e8f0f2;
  --el-fill-color-darker: #eef4f5;
  --el-mask-color: rgba(243, 247, 248, .72);
  --el-border-radius-base: 4px;
  --el-border-radius-small: 2px;
  --el-font-family: var(--sans);
}

html.dark {
  /* ---- dark：深空工业 HUD ---- */
  --bg: #05080d;
  --bg-sub: #070c13;
  --bg-inset: #0b121c;
  --surface: #0a1018;
  --surface-2: rgba(7, 13, 21, .74);
  --panel: linear-gradient(160deg, rgba(13, 22, 32, .94), rgba(6, 11, 18, .94));

  --line: rgba(126, 196, 208, .13);
  --line-2: rgba(126, 196, 208, .22);
  --line-3: rgba(126, 196, 208, .34);

  --text: #e8f4f6;
  --text-2: #9fb4be;
  --text-3: #607680;

  --accent: #5ef2d6;
  --accent-2: #9dffcf;
  --accent-soft: rgba(94, 242, 214, .09);
  --accent-line: rgba(94, 242, 214, .30);
  --ice: #58a6ff;
  --violet: #ff3d81;
  --acid: #d7ff5e;
  --ok: #5ef2d6;
  --warn: #f4d35e;
  --danger: #ff4d7e;

  --on-accent: #03110d;
  --on-remote: #12040a;
  --auth-card: rgba(7, 13, 21, .80);
  --auth-border: rgba(94, 242, 214, .18);
  --remote-card: rgba(8, 12, 19, .82);
  --remote-border: rgba(255, 61, 129, .22);

  --grid-line: rgba(94, 242, 214, .075);
  --scan-line: rgba(255, 255, 255, .03);
  --scan-beam: rgba(94, 242, 214, .10);
  --glow-a: rgba(94, 242, 214, .16);
  --glow-b: rgba(88, 166, 255, .13);
  --glow-c: rgba(255, 61, 129, .09);
  --overlay: rgba(4, 7, 11, .66);

  --shadow-xs: inset 0 1px 0 rgba(255, 255, 255, .03);
  --shadow-sm: inset 0 1px 0 rgba(255, 255, 255, .03), 0 8px 24px -12px rgba(0, 0, 0, .8);
  --shadow-md: inset 0 1px 0 rgba(255, 255, 255, .04), 0 18px 42px -18px rgba(0, 0, 0, .9);
  --shadow-lg: inset 0 1px 0 rgba(255, 255, 255, .05), 0 30px 80px -24px rgba(0, 0, 0, .95), 0 0 60px -24px rgba(94, 242, 214, .25);

  /* Element Plus dark */
  --el-color-primary: #5ef2d6;
  --el-color-primary-light-3: #8bffe7;
  --el-color-primary-light-5: #b4fff0;
  --el-color-primary-light-7: #d8fff8;
  --el-color-primary-light-8: #e8fffb;
  --el-color-primary-light-9: #f2fffd;
  --el-color-primary-dark-2: #2fc7ad;
  --el-color-success: #5ef2d6;
  --el-color-warning: #f4d35e;
  --el-color-danger: #ff4d7e;
  --el-color-info: #8fa6b2;
  --el-bg-color: #0a1018;
  --el-bg-color-page: #05080d;
  --el-bg-color-overlay: #0c141e;
  --el-text-color-primary: #e8f4f6;
  --el-text-color-regular: #9fb4be;
  --el-text-color-secondary: #607680;
  --el-text-color-placeholder: #4d6069;
  --el-border-color: rgba(126, 196, 208, .22);
  --el-border-color-light: rgba(126, 196, 208, .15);
  --el-border-color-lighter: rgba(126, 196, 208, .09);
  --el-fill-color-blank: #0a1018;
  --el-fill-color-light: #0e1722;
  --el-fill-color: #101b27;
  --el-fill-color-darker: #0b121c;
  --el-mask-color: rgba(2, 5, 9, .78);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { height: 100%; }
html { color-scheme: light; }
html.dark { color-scheme: dark; }
body {
  font-family: var(--sans);
  color: var(--text);
  background: var(--bg);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
  transition: background-color .35s var(--ease-2), color .35s var(--ease-2);
}
#app { min-height: 100%; }

h1, h2, h3, .display { font-family: var(--display); letter-spacing: -.03em; }

::selection { background: color-mix(in srgb, var(--accent) 30%, transparent); color: var(--text); }

/* ============ HUD 背景层 ============ */
.mesh {
  background-color: var(--bg-sub);
  background-image:
    linear-gradient(var(--grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--grid-line) 1px, transparent 1px),
    radial-gradient(900px 520px at 12% -10%, var(--glow-a), transparent 60%),
    radial-gradient(760px 480px at 92% 4%, var(--glow-b), transparent 62%),
    radial-gradient(820px 520px at 60% 110%, var(--glow-c), transparent 60%);
  background-size: 48px 48px, 48px 48px, auto, auto, auto;
  background-attachment: fixed, fixed, fixed, fixed, fixed;
  animation: meshDrift 5s ease-in-out infinite alternate;
  will-change: background-position;
}
@keyframes meshDrift {
  from {
    background-position: 0 0, 0 0, 0% 0%, 100% 0%, 50% 100%;
  }
  to {
    background-position: 48px 48px, 48px 48px, 7% 4%, 93% 7%, 50% 93%;
  }
}

.noise { position: relative; }
.noise::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  background-image:
    repeating-linear-gradient(0deg, var(--scan-line) 0 1px, transparent 1px 4px),
    linear-gradient(180deg, transparent 0%, var(--scan-beam) 46%, var(--scan-beam) 54%, transparent 100%);
  background-size: 100% 4px, 100% 38%;
  background-repeat: repeat, no-repeat;
  animation: scanBeam 3.6s linear infinite;
  opacity: .85;
  transition: opacity .35s var(--ease-2);
}
@keyframes scanBeam {
  from { background-position: 0 0, 0 -45%; }
  to { background-position: 0 0, 0 145%; }
}
.noise::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: .35;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='.045'/%3E%3C/svg%3E");
  transition: opacity .35s var(--ease-2);
}
html.dark .noise::after { opacity: .5; }
.noise > * { position: relative; z-index: 1; }

.glass {
  background: var(--surface-2);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  transition: background-color .35s var(--ease-2);
}

.surface {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-xs);
}

/* HUD 角标 */
.hud-corners { position: relative; }
.hud-corners::before,
.hud-corners::after {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  pointer-events: none;
  border-color: var(--accent);
  opacity: .55;
}
.hud-corners::before { top: -1px; left: -1px; border-top: 1px solid; border-left: 1px solid; }
.hud-corners::after { bottom: -1px; right: -1px; border-bottom: 1px solid; border-right: 1px solid; }

/* ============ 呼吸感 ============ */
.reveal {
  opacity: 0;
  transform: translateY(18px);
  filter: blur(3px);
  transition: opacity var(--dur-3) var(--ease), transform var(--dur-3) var(--ease), filter var(--dur-3) var(--ease);
  will-change: opacity, transform, filter;
}
.reveal.is-visible { opacity: 1; transform: none; filter: none; }

@keyframes hudPulse {
  0%, 100% { opacity: .5; }
  50% { opacity: 1; }
}
@keyframes scanDown {
  from { transform: translateY(-100%); }
  to { transform: translateY(100vh); }
}
@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@media (prefers-reduced-motion: reduce) {
  .reveal { opacity: 1 !important; transform: none !important; filter: none !important; transition: none !important; }
  * { animation-duration: .01ms !important; transition-duration: .01ms !important; }
}

/* ============ 工具类 ============ */
.mono { font-family: var(--mono); }

.u-label {
  font-family: var(--mono);
  font-size: 10px;
  letter-spacing: .16em;
  text-transform: uppercase;
  color: var(--text-3);
}

.u-hint { font-size: 12px; color: var(--text-3); }

.page-head h2 {
  font-family: var(--display);
  font-weight: 700;
  letter-spacing: -.035em;
}

/* ============ Element Plus 双主题校准 ============ */
.el-input__wrapper,
.el-textarea__inner,
.el-select__wrapper {
  background-color: var(--surface) !important;
  box-shadow: 0 0 0 1px var(--line-2) inset !important;
  color: var(--text) !important;
  transition: box-shadow .2s var(--ease-2), background-color .35s var(--ease-2);
}
.el-input__wrapper:hover,
.el-textarea__inner:hover,
.el-select__wrapper:hover { box-shadow: 0 0 0 1px var(--line-3) inset !important; }
.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-select__wrapper.is-focused {
  box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-soft) !important;
}
.el-input__inner,
.el-textarea__inner { color: var(--text) !important; }
.el-input__inner::placeholder,
.el-textarea__inner::placeholder { color: var(--text-3) !important; }

.el-button {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--line-2);
  --el-button-text-color: var(--text-2);
  --el-button-hover-bg-color: var(--accent-soft);
  --el-button-hover-border-color: var(--accent-line);
  --el-button-hover-text-color: var(--accent);
  --el-button-active-bg-color: var(--accent-soft);
  --el-button-active-border-color: var(--accent);
  --el-button-active-text-color: var(--accent);
  border-radius: 3px;
}
.el-button--primary {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
  --el-button-text-color: var(--on-accent);
  --el-button-hover-bg-color: var(--accent-2);
  --el-button-hover-border-color: var(--accent-2);
  --el-button-hover-text-color: var(--on-accent);
  --el-button-active-bg-color: var(--accent);
  --el-button-active-border-color: var(--accent);
  --el-button-active-text-color: var(--on-accent);
  box-shadow: 0 0 22px -8px color-mix(in srgb, var(--accent) 65%, transparent);
}
.el-button--danger {
  --el-button-bg-color: transparent;
  --el-button-border-color: color-mix(in srgb, var(--danger) 42%, transparent);
  --el-button-text-color: var(--danger);
}

.el-dialog,
.el-message-box {
  background: var(--panel);
  border: 1px solid var(--line-2);
  border-radius: 6px;
  box-shadow: var(--shadow-lg);
}
.el-dialog__title,
.el-message-box__title { color: var(--text); font-family: var(--display); }
.el-dialog__header { border-bottom: 1px solid var(--line); padding-bottom: 12px; }
.el-dialog__footer { border-top: 1px solid var(--line); padding-top: 12px; }

.el-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--bg-inset);
  --el-table-border-color: var(--line);
  --el-table-row-hover-bg-color: var(--accent-soft);
  --el-table-text-color: var(--text-2);
  --el-table-header-text-color: var(--text-3);
  background: transparent;
}
.el-table th.el-table__cell,
.el-table tr,
.el-table td.el-table__cell { background: transparent; }
.el-table th.el-table__cell {
  background: var(--bg-inset);
  text-transform: uppercase;
  letter-spacing: .1em;
}
.el-table__empty-text { color: var(--text-3); }

.el-empty__image { filter: grayscale(1) contrast(.9); opacity: .55; }
html.dark .el-empty__image { filter: grayscale(1) brightness(.65) sepia(.35) hue-rotate(120deg); opacity: .45; }
.el-empty__description p {
  color: var(--text-3);
  font-family: var(--mono);
  font-size: 10.5px;
  letter-spacing: .08em;
}
.el-loading-mask { background-color: var(--overlay); }
.el-loading-spinner .path { stroke: var(--accent); }

.el-message {
  background: var(--surface);
  border: 1px solid var(--line-2);
  box-shadow: var(--shadow-md);
}
.el-message__content { color: var(--text); }

.el-dropdown__popper,
.el-select__popper,
.el-popper.is-light,
.el-picker__popper {
  background: var(--surface) !important;
  border: 1px solid var(--line-2) !important;
  color: var(--text) !important;
}
.el-dropdown-menu__item:not(.is-disabled):hover { background: var(--accent-soft); color: var(--accent); }
.el-select-dropdown__item.is-hovering { background: var(--accent-soft); }
.el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: var(--text-2);
  --el-pagination-button-color: var(--text-2);
}
.el-pagination button:hover { color: var(--accent); }

input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus {
  -webkit-text-fill-color: var(--text);
  -webkit-box-shadow: 0 0 0 1000px var(--surface) inset;
  transition: background-color 9999s ease-out 0s;
}

/* 强制 Element Plus 跟随当前主题（EP 默认变量在部分构建顺序下会覆盖 :root） */
html {
  --el-color-primary: var(--accent) !important;
  --el-color-success: var(--ok) !important;
  --el-color-warning: var(--warn) !important;
  --el-color-danger: var(--danger) !important;
  --el-bg-color: var(--surface) !important;
  --el-bg-color-page: var(--bg) !important;
  --el-bg-color-overlay: var(--surface) !important;
  --el-text-color-primary: var(--text) !important;
  --el-text-color-regular: var(--text-2) !important;
  --el-text-color-secondary: var(--text-3) !important;
  --el-border-color: var(--line-2) !important;
  --el-border-color-light: var(--line) !important;
  --el-border-color-lighter: var(--line) !important;
  --el-fill-color-blank: var(--surface) !important;
  --el-fill-color-light: var(--bg-inset) !important;
  --el-fill-color: var(--bg-inset) !important;
  --el-mask-color: var(--overlay) !important;
}
.el-button--primary:not(.is-plain):not(.is-link):not(.is-text) {
  background-color: var(--accent) !important;
  border-color: var(--accent) !important;
  color: var(--on-accent) !important;
}
.el-button--primary:not(.is-plain):not(.is-link):not(.is-text):hover {
  background-color: var(--accent-2) !important;
  border-color: var(--accent-2) !important;
  color: var(--on-accent) !important;
}
.el-button--primary.is-link,
.el-button--primary.is-text { color: var(--accent) !important; }

/* ============ 滚动条 ============ */
::-webkit-scrollbar { width: 10px; height: 10px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--text-3) 32%, transparent);
  border-radius: 999px;
  border: 3px solid transparent;
  background-clip: content-box;
}
::-webkit-scrollbar-thumb:hover { background: color-mix(in srgb, var(--accent) 48%, transparent); background-clip: content-box; }
</style>
