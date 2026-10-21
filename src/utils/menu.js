export function toTree(rows, parentId = null) {
  return rows
    .filter(row => (row.parentId == null ? null : row.parentId) === parentId)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0) || a.id - b.id)
    .map(row => ({ ...row, children: toTree(rows, row.id) }))
}

export function navigation(rows) {
  const typeOrder = { DIRECTORY: 0, MENU: 1, BUTTON: 2 }
  return toTree(rows)
    .filter(node => node.type !== 'BUTTON')
    .sort((a, b) => typeOrder[a.type] - typeOrder[b.type])
}
