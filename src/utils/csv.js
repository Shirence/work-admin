function cell(value) {
  let text = value === null || value === undefined ? '' : String(value)
  // 防止 CSV 公式执行：以 = + - @ 或制表/回车开头的值前置单引号。
  if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`
  if (/[",\n\r]/.test(text)) text = `"${text.replace(/"/g, '""')}"`
  return text
}

export function downloadCsv(filename, headers, rows) {
  const lines = [headers.map(cell).join(','), ...rows.map(row => row.map(cell).join(','))]
  const blob = new Blob([`\uFEFF${lines.join('\r\n')}`], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
