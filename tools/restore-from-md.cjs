const fs = require('node:fs')
const path = require('node:path')
const [lessonPath, destination] = process.argv.slice(2)
if (!lessonPath || !destination) {
  console.error('用法: node tools/restore-from-md.cjs <课件.md> <目标目录>')
  process.exit(1)
}
const root = path.resolve(destination)
const lines = fs.readFileSync(path.resolve(lessonPath), 'utf8').replace(/\r\n/g, '\n').split('\n')
const files = []
const seen = new Set()
for (let i = 0; i < lines.length; i++) {
  const marker = /^<!-- project-file: (.+) -->$/.exec(lines[i])
  if (!marker) continue
  const relative = marker[1]
  if (relative.includes('\\') || relative.startsWith('/') || relative.split('/').some(p => !p || p === '.' || p === '..') || seen.has(relative)) {
    throw new Error(`无效或重复文件路径: ${relative}`)
  }
  seen.add(relative)
  const opening = /^(`{3,})[\w-]*$/.exec(lines[++i] || '')
  if (!opening) throw new Error(`文件没有完整代码块: ${relative}`)
  const content = []
  while (++i < lines.length && lines[i] !== opening[1]) content.push(lines[i])
  if (i === lines.length) throw new Error(`代码块未闭合: ${relative}`)
  files.push({ relative, content: content.join('\n') + '\n' })
}
if (!files.length) throw new Error('未找到 project-file 标记，请使用完整课件')
// Validate all targets before creating any project files.
for (const file of files) {
  const target = path.resolve(root, file.relative)
  if (!target.startsWith(root + path.sep)) throw new Error('目标路径越界')
  let cursor = root
  if (fs.existsSync(cursor) && fs.lstatSync(cursor).isSymbolicLink()) throw new Error('目标目录不能是符号链接')
  for (const segment of file.relative.split('/')) {
    cursor = path.join(cursor, segment)
    if (fs.existsSync(cursor) && fs.lstatSync(cursor).isSymbolicLink()) throw new Error(`拒绝符号链接: ${cursor}`)
  }
  if (fs.existsSync(target) && (!fs.statSync(target).isFile() || fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n') !== file.content)) {
    throw new Error(`文件已存在且内容不同，请使用空目录: ${file.relative}`)
  }
}
for (const file of files) {
  const target = path.resolve(root, file.relative)
  fs.mkdirSync(path.dirname(target), { recursive: true })
  if (!fs.existsSync(target)) fs.writeFileSync(target, file.content, { flag: 'wx' })
}
console.log(`已还原 ${files.length} 个文件到 ${root}`)
console.log('下一步: pnpm install --frozen-lockfile，然后按课件配置 .env.local 并执行 pnpm serve')
