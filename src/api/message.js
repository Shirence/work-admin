import store from '../store'
import { expireSession } from './request'
import { createSseParser } from '../utils/sse'
export async function streamMessage(body, { signal, onEvent = () => {} } = {}) {
    const token = store.state.token
    if (!token) throw new Error('请先登录后再使用 AI 对话')
    const controller = new AbortController()
    const abort = () => controller.abort()
    signal?.addEventListener('abort', abort, { once: true })
    if (signal?.aborted) abort()
    let timer, reader, timedOut = false,
        completed = false
    const resetTimer = () => {
        clearTimeout(timer)
        timer = setTimeout(() => {
            timedOut = true;
            controller.abort()
        }, 45000)
    }
    resetTimer()
    try {
        const base = (process.env.VUE_APP_API_BASE_URL || '/').replace(/\/$/, '')
        const response = await fetch(`${base}/api/messages`, {
            method: 'POST',
            signal: controller.signal,
            headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream, application/json', satoken: token },
            body: JSON.stringify(body)
        })
        if (token !== store.state.token) throw new Error('登录账号已变更')
        controller.signal.throwIfAborted()
        if (!response.ok) {
            if (response.status === 401) expireSession(token)
            const body = await response.json().catch(() => null)
            const fallback = {
                401: '登录已失效，请重新登录',
                403: '当前账号没有 AI 对话权限',
                429: '请求过于频繁，请稍后重试',
                503: 'AI 服务尚未配置或暂不可用，请联系管理员'
            }
            const error = new Error(body?.message || fallback[response.status] || `请求失败（${response.status}）`)
            error.status = response.status
            throw error
        }
        if (!response.headers.get('Content-Type')?.toLowerCase().includes('text/event-stream') || !response.body) throw new Error('后端未返回流式回答，请检查 AI 接口配置')
        const parse = createSseParser((event, data) => {
            if (completed) return
            controller.signal.throwIfAborted()
            if (token !== store.state.token) throw new Error('登录账号已变更')
            if (!data || typeof data !== 'object' || Array.isArray(data)) throw new Error('AI 返回的数据格式不正确')
            if (event === 'error') throw new Error(data.message || '生成失败，请稍后重试')
            if (event === 'delta' && typeof data.text !== 'string') throw new Error('AI 返回的文本格式不正确')
            if (event === 'done') {
                if (data.status !== 'completed') throw new Error('回复未完成')
                completed = true
            }
            onEvent(event, data)
        })
        reader = response.body.getReader()
        const decoder = new TextDecoder()
        while (!completed) {
            const { value, done } = await reader.read()
            controller.signal.throwIfAborted()
            if (token !== store.state.token) throw new Error('登录账号已变更')
            resetTimer()
            parse(done ? decoder.decode() : decoder.decode(value, { stream: true }))
            if (done) break
        }
        if (!completed) throw new Error('连接中断，回复未完成')
    } catch (error) {
        if (timedOut) throw new Error('45 秒未收到数据，请重试')
        if (error instanceof TypeError && !controller.signal.aborted) throw new Error('网络连接失败，请检查网络或后端服务')
        throw error
    } finally {
        clearTimeout(timer);
        signal?.removeEventListener('abort', abort)
        if (reader) {
            await reader.cancel().catch(() => {});
            reader.releaseLock()
        }
        controller.abort()
    }
}
