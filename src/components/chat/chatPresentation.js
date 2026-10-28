import { messageKey } from '../../utils/chat'

export function initials(name) { return Array.from(String(name || '群')).slice(0, 1).join('') }

export function avatarColor(id) {
  const colors = ['#7ba7a0', '#809bc5', '#b49a7e', '#9c93b5', '#80a681', '#c28f8b']
  const hash = Array.from(String(id || '')).reduce((value, char) => value + char.charCodeAt(0), 0)
  return colors[hash % colors.length]
}

export function timeLabel(value, compact = false) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const day = item => new Date(item.getFullYear(), item.getMonth(), item.getDate()).getTime()
  const days = Math.round((day(now) - day(date)) / 86400000)
  const clock = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  if (days === 0) return clock
  if (days === 1) return compact ? '昨天' : `昨天 ${clock}`
  const label = date.toLocaleDateString('zh-CN', { ...(date.getFullYear() !== now.getFullYear() ? { year: 'numeric' } : {}), month: 'numeric', day: 'numeric' })
  return compact ? label : `${label} ${clock}`
}

export function timelineEntries(messages) {
  return messages.map((message, index) => {
    const previous = messages[index - 1]
    const time = new Date(message.sentAt).getTime()
    const previousTime = previous ? new Date(previous.sentAt).getTime() : NaN
    return { message, key: messageKey(message), time: timeLabel(message.sentAt), showTime: !previous || Math.abs(time - previousTime) > 300000 }
  })
}
