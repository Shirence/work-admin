const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')
const { code } = transformSync(readFileSync(require('node:path').join(__dirname, '../src/utils/markdown.js'), 'utf8'), {
  configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs']
})
const target = { exports: {} }
new Function('require', 'module', 'exports', code)(require, target, target.exports)
const { renderMarkdown } = target.exports

test('renders headings, nested lists, emphasis, quotations and inline code as document elements', () => {
  const html = renderMarkdown('# 文档标题\n\n## 第二节\n\n**重点**与 `satoken`\n\n1. 第一项\n   - 子项目\n\n> 提示内容\n\n---')
  for (const fragment of ['<h1>文档标题</h1>', '<h2>第二节</h2>', '<strong>重点</strong>', '<code>satoken</code>', '<ol>', '<ul>', '<blockquote>', '<hr>']) assert.ok(html.includes(fragment), fragment)
})

test('renders aligned Markdown tables inside a keyboard-accessible scrolling region', () => {
  const html = renderMarkdown('| 协议 | 方向 |\n| :--- | ---: |\n| HTTP | 请求响应 |\n| WS | 双向 |')
  assert.match(html, /class="md-table-scroll" tabindex="0" role="region"/)
  assert.match(html, /<table>/)
  assert.match(html, /<th style="text-align:right">方向<\/th>/)
  assert.match(html, /<td style="text-align:right">双向<\/td>/)
  assert.ok(html.endsWith('</table></div>\n'))
})

test('code blocks have syntax coloring, language label, copy button and escaped source', () => {
  const html = renderMarkdown('```js\nconst text = "<img src=x onerror=alert(1)>";\n```')
  assert.match(html, /class="hljs-keyword">const<\/span>/)
  assert.match(html, /<span>js<\/span>/)
  assert.match(html, /<button type="button" class="md-copy" aria-label="复制代码">/)
  assert.match(html, /&lt;img/)
  assert.doesNotMatch(html, /<img|<script/i)
})

test('unknown and streaming unclosed code fences remain readable and escaped', () => {
  for (const source of ['```unknown\n<script>alert(1)</script>', '```js\nconst value = "未完成', '    <svg onload=alert(1)>\n']) {
    const html = renderMarkdown(source)
    assert.match(html, /class="md-code-block"/)
    assert.match(html, /<\/code><\/pre><\/div>/)
    assert.doesNotMatch(html, /<script|<svg/i)
  }
})

test('raw HTML, event handlers and injected fence labels cannot become executable markup', () => {
  const html = renderMarkdown('<img src=x onerror="alert(1)">\n\n<script>alert(1)</script>\n\n```a"><img/src=x/onerror=alert(1)>\nhello\n```')
  assert.match(html, /&lt;img/)
  assert.doesNotMatch(html, /<img|<script|<svg|<iframe|onerror="alert/i)
})

test('dangerous URLs are rejected while web links open safely in a new tab', () => {
  const html = renderMarkdown('[网站](https://example.com) [坏链接](javascript:alert%281%29) [编码](jav&#x61;script:alert%281%29) [文件](file:///etc/passwd)')
  assert.match(html, /href="https:\/\/example.com" target="_blank" rel="noopener noreferrer"/)
  assert.doesNotMatch(html, /href="(?:javascript|file|data|vbscript):/i)
})

test('remote image Markdown is a user-opened link instead of an automatic image request', () => {
  const html = renderMarkdown('![示意图](https://example.com/diagram.png) ![内嵌](data:image/png;base64,AAAA)')
  assert.match(html, /查看图片：示意图/)
  assert.match(html, /href="https:\/\/example.com\/diagram.png"/)
  assert.doesNotMatch(html, /<img|href="data:/i)
})

test('streamed partial Markdown can render at each character boundary', () => {
  const text = '## 中文😊\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n```json\n{"name":"测试"}\n```'
  for (let i = 1; i <= text.length; i++) assert.equal(typeof renderMarkdown(text.slice(0, i)), 'string')
  assert.equal(renderMarkdown(null), '')
})
