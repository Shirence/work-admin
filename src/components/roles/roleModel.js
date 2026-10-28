export function rolePayload(form, original = null) {
  const payload = {
    name: (form.name || '').trim(),
    code: (original?.builtin ? original.code : form.code || '').trim(),
    description: (form.description || '').trim(),
    enabled: original?.builtin ? original.enabled : form.enabled
  }
  if (!payload.name || payload.name.length > 100) throw new Error('角色名必填，最多 100 个字符')
  if (!/^[a-zA-Z][a-zA-Z0-9_:-]{0,63}$/.test(payload.code)) {
    throw new Error('编码以英文字母开头，只能包含字母、数字、下划线、冒号或连字符，最多 64 个字符')
  }
  if (payload.description.length > 255) throw new Error('说明最多 255 个字符')
  if (typeof payload.enabled !== 'boolean') throw new Error('请选择角色启用状态')
  return payload
}

export function deletionReason(role) {
  if (role.builtin) return '内置角色不能删除'
  if (role.userCount > 0) return '角色已绑定用户，请先解除用户绑定'
  return ''
}

export function permissionKeys(tree) {
  return tree.flatMap(node => [node.id, ...permissionKeys(node.children || [])])
}
