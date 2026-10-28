import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'
import http from 'highlight.js/lib/languages/http'

const languages = { javascript, typescript, xml, css, json, bash, python, java, sql, yaml, http }
for (const [name, definition] of Object.entries(languages)) hljs.registerLanguage(name, definition)
hljs.registerAliases('vue', { languageName: 'xml' })

// 不接受 Markdown 内嵌 HTML；代码和自定义属性都经过转义后才进入生成的 HTML。
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true, typographer: false })
const escape = markdown.utils.escapeHtml

function codeBlock(content, info = '') {
  const language = info.trim().split(/\s+/)[0].toLowerCase().slice(0, 40)
  let highlighted = escape(content)
  if (language && hljs.getLanguage(language)) {
    try { highlighted = hljs.highlight(content, { language, ignoreIllegals: true }).value }
    catch { /* 未完成的流式代码也应保留可读的纯文本。 */ }
  }
  return `<div class="md-code-block"><div class="md-code-toolbar"><span>${escape(language || '纯文本')}</span><button type="button" class="md-copy" aria-label="复制代码">复制代码</button></div><pre tabindex="0" aria-label="代码块"><code class="hljs">${highlighted}</code></pre></div>\n`
}

markdown.renderer.rules.fence = (tokens, index) => codeBlock(tokens[index].content, tokens[index].info)
markdown.renderer.rules.code_block = (tokens, index) => codeBlock(tokens[index].content)
markdown.renderer.rules.table_open = () => '<div class="md-table-scroll" tabindex="0" role="region" aria-label="数据表格"><table>\n'
markdown.renderer.rules.table_close = () => '</table></div>\n'
const renderLink = markdown.renderer.rules.link_open || ((tokens, index, options, env, self) => self.renderToken(tokens, index, options))
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet('target', '_blank')
  tokens[index].attrSet('rel', 'noopener noreferrer')
  return renderLink(tokens, index, options, env, self)
}
// 图片以链接展示，由用户主动打开，避免回答流自动请求第三方图片。
markdown.renderer.rules.image = (tokens, index) => {
  const token = tokens[index]
  const label = escape(token.content || '图片')
  const src = token.attrGet('src') || ''
  return /^https?:\/\//i.test(src) && markdown.validateLink(src) ?
    `<a href="${escape(src)}" target="_blank" rel="noopener noreferrer">查看图片：${label}</a>` : label
}

export function renderMarkdown(content) {
  return markdown.render(typeof content === 'string' ? content : '')
}
