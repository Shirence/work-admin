export const typeLabels = { DIRECTORY: '目录', MENU: '页面', BUTTON: '按钮' }

export function parentCandidates(form, rows, id = null) {
  const excluded = new Set(id == null ? [] : [id])
  let size = -1
  while (size !== excluded.size) {
    size = excluded.size
    rows.forEach(row => { if (excluded.has(row.parentId)) excluded.add(row.id) })
  }
  const parentType = form.type === 'BUTTON' ? 'MENU' : 'DIRECTORY'
  return rows.filter(row => row.type === parentType && !excluded.has(row.id))
}

export function menuPayload(form, original, rows) {
  // 内置节点只允许名称和排序变化，不能通过表单事件篡改结构与权限。
  const structural = original?.builtin ? original : form
  const type = structural.type
  const body = {
    name: (form.name || '').trim(), type,
    parentId: structural.parentId === '' ? null : structural.parentId ?? null,
    path: type === 'MENU' ? (structural.path || '').trim() : null,
    permission: type === 'DIRECTORY' ? null : (structural.permission || '').trim(),
    sortOrder: form.sortOrder, enabled: structural.enabled
  }
  if (!body.name || body.name.length > 100) throw new Error('节点名称必填，最多 100 个字符')
  if (!Object.hasOwn(typeLabels, type)) throw new Error('请选择有效的节点类型')
  if (!Number.isInteger(body.sortOrder) || body.sortOrder < 0 || body.sortOrder > 9999) throw new Error('排序必须为 0～9999 的整数')
  if (typeof body.enabled !== 'boolean') throw new Error('请选择节点启用状态')
  if (type === 'BUTTON' && body.parentId === null) throw new Error('按钮必须归属一个页面')
  if (body.parentId !== null && !parentCandidates(body, rows, original?.id).some(row => row.id === body.parentId)) {
    throw new Error('上级节点不合法，不能选择自身或下级节点')
  }
  const children = original ? rows.filter(row => row.parentId === original.id) : []
  if (children.some(child => child.type === 'BUTTON' ? type !== 'MENU' : type !== 'DIRECTORY')) {
    throw new Error('现有子节点与新类型不兼容，请先调整子节点')
  }
  if (type === 'MENU' && (body.path.length > 200 || !/^\/(?!\/)[^\s?#\\]*$/.test(body.path))) {
    throw new Error('请输入以 / 开头的页面路径，最多 200 个字符')
  }
  if (type !== 'DIRECTORY') {
    if (!body.permission || body.permission.length > 100) throw new Error('权限标识必填，最多 100 个字符')
    if (rows.some(row => row.id !== original?.id && row.permission === body.permission)) throw new Error('权限标识已存在，请使用唯一标识')
  }
  return body
}

export function deletionReason(node, rows = []) {
  if (node.builtin) return '内置节点不能删除'
  if (node.children?.length || rows.some(row => row.parentId === node.id)) return '请先删除下级节点'
  return ''
}

export function hasDisabledAncestor(node, rows) {
  const visited = new Set([node.id])
  let id = node.parentId
  while (id != null && !visited.has(id)) {
    visited.add(id)
    const parent = rows.find(row => row.id === id)
    if (!parent) return false
    if (!parent.enabled) return true
    id = parent.parentId
  }
  return false
}
