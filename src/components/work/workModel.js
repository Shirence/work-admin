const states = [{ value: 'todo', label: '待开始' }, { value: 'doing', label: '进行中' }, { value: 'done', label: '已完成' }]
const priorities = [{ value: 'low', label: '低' }, { value: 'medium', label: '中' }, { value: 'high', label: '高' }]
function monday() {
  const day = new Date(), weekday = day.getDay() || 7
  day.setDate(day.getDate() - weekday + 1)
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`
}
export const workModules = {
  tasks: {
    title: '任务管理', description: '分配工作、跟进进度与截止日期，让每一项协作都有结果。', singular: '任务',
    create: 'task:create', edit: 'task:update', remove: 'task:delete',
    defaults: () => ({ title: '', description: '', status: 'todo', priority: 'medium', assigneeId: null, department: '未分组', dueDate: null }),
    statuses: states,
    fields: [{ key: 'title', label: '任务标题', max: 200, required: true }, { key: 'description', label: '任务说明', type: 'textarea', max: 10000 }, { key: 'status', label: '状态', options: states }, { key: 'priority', label: '优先级', options: priorities }, { key: 'assigneeId', label: '负责人', people: true }, { key: 'department', label: '部门', max: 100, required: true }, { key: 'dueDate', label: '截止日期', type: 'date' }],
    columns: [{ key: 'title', label: '任务', min: 210 }, { key: 'status', label: '状态', options: states }, { key: 'priority', label: '优先级', options: priorities }, { key: 'assigneeName', label: '负责人' }, { key: 'department', label: '部门' }, { key: 'dueDate', label: '截止日期', min: 130 }]
  },
  announcements: {
    title: '公告中心', description: '发布团队消息与重要安排，支持 Markdown 正文和发布状态管理。', singular: '公告',
    create: 'announcement:manage', edit: 'announcement:manage', remove: 'announcement:manage',
    defaults: () => ({ title: '', content: '', important: false, published: true }),
    fields: [{ key: 'title', label: '公告标题', max: 200, required: true }, { key: 'content', label: '公告正文（Markdown）', type: 'textarea', max: 20000, required: true }, { key: 'important', label: '重要公告', type: 'switch' }, { key: 'published', label: '立即发布', type: 'switch' }],
    columns: [{ key: 'title', label: '公告标题', min: 260 }, { key: 'important', label: '重要', boolean: true }, { key: 'published', label: '已发布', boolean: true }, { key: 'authorName', label: '发布人' }, { key: 'updatedAt', label: '更新时间', date: true, min: 170 }]
  },
  reports: {
    title: '工作周报', description: '记录工作成果、下周计划和待解决问题，每人每周一份，支持草稿与提交。', singular: '周报',
    create: 'report:access', edit: 'report:access', remove: 'report:access',
    defaults: () => ({ week: monday(), summary: '', plan: '', blockers: '', status: 'draft' }),
    statuses: [{ value: 'draft', label: '草稿' }, { value: 'submitted', label: '已提交' }],
    fields: [{ key: 'week', label: '周起始日期（周一）', type: 'date', required: true }, { key: 'summary', label: '本周总结', type: 'textarea', max: 10000, required: true }, { key: 'plan', label: '下周计划', type: 'textarea', max: 10000, required: true }, { key: 'blockers', label: '风险与协助', type: 'textarea', max: 5000 }, { key: 'status', label: '状态', options: [{ value: 'draft', label: '草稿' }, { value: 'submitted', label: '已提交' }] }],
    columns: [{ key: 'week', label: '周起始日期', min: 140 }, { key: 'authorName', label: '成员' }, { key: 'summary', label: '本周总结', min: 280 }, { key: 'status', label: '状态', options: [{ value: 'draft', label: '草稿' }, { value: 'submitted', label: '已提交' }] }, { key: 'updatedAt', label: '更新时间', date: true, min: 170 }]
  }
}
export function workPayload(config, form) {
  const body = {}
  for (const field of config.fields) {
    const value = typeof form[field.key] === 'string' ? form[field.key].trim() : form[field.key]
    if (field.required && (value === null || value === undefined || value === '')) throw new Error(`请填写${field.label}`)
    if (typeof value === 'string' && field.max && value.length > field.max) throw new Error(`${field.label}最多 ${field.max} 字符`)
    body[field.key] = value
  }
  return body
}
export const dateTime = value => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
export function workCell(column, row) {
  if (column.options) return column.options.find(option => option.value === row[column.key])?.label || row[column.key] || ''
  if (column.boolean) return row[column.key] ? '是' : '否'
  if (column.date) return dateTime(row[column.key])
  return row[column.key] || (column.key === 'assigneeName' ? row.assigneeAccount : '') || ''
}
