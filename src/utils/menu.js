export function toTree(rows, parentId = null) {
    const children = new Map()
    for (const row of rows) {
        if (!children.has(row.parentId)) children.set(row.parentId, [])
        children.get(row.parentId).push(row)
    }
    for (const group of children.values()) group.sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
    const visiting = new Set()
    function build(id) {
        return (children.get(id) || []).filter(row => !visiting.has(row.id)).map(row => {
            visiting.add(row.id)
            const node = { ...row, children: build(row.id) }
            visiting.delete(row.id)
            return node
        })
    }
    return build(parentId)
}
// 后端存的是原生 HTML 页面地址，Vue 需要显式映射到已有组件。
export const pageRoutes = {
    '/tasks.html': '/tasks',
    '/announcements.html': '/announcements',
    '/reports.html': '/reports',
    '/notifications.html': '/notifications',
    '/users.html': '/user',
    '/roles.html': '/roles',
    '/menus.html': '/menus',
    '/chat.html': '/chat',
    '/': '/messages'
}
export function navigation(rows) {
    function convert(node) {
        if (node.type === 'BUTTON') return null
        const children = node.children.map(convert).filter(Boolean)
        if (node.type === 'DIRECTORY') return children.length ? {...node, children } : null
        const route = pageRoutes[node.path]
        return route ? {...node, route, children: [] } : null
    }
    return toTree(rows).map(convert).filter(Boolean)
}
