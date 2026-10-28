const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const { transformSync } = require('@babel/core')

function load(file, overrides = {}, globals = {}) {
  const code = transformSync(readFileSync(require('node:path').join(__dirname, '..', 'src', file), 'utf8'), {
    configFile: false, babelrc: false, plugins: ['@babel/plugin-transform-modules-commonjs']
  }).code
  const module = { exports: {} }
  new Function('require', 'module', 'exports', ...Object.keys(globals), code)(id =>
    Object.hasOwn(overrides, id) ? overrides[id] : require(id), module, module.exports, ...Object.values(globals))
  return module.exports
}
const sse = load('utils/sse.js')
const frame = (event, data) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`
const encoder = new TextEncoder()
function stream(parts) {
  return new ReadableStream({ start(controller) { for (const part of parts) controller.enqueue(typeof part === 'string' ? encoder.encode(part) : part); controller.close() } })
}
function client(fetchImpl, state = { token: 'session' }) {
  const expired = []
  const api = load('api/message.js', { '../store': { state }, './request': { expireSession: token => expired.push(token) }, '../utils/sse': sse }, { fetch: fetchImpl })
  return { ...api, state, expired }
}

test('SSE preserves split frames, CRLF, multiline data and UTF-8 Chinese/emoji', () => {
  const events = []
  const parse = sse.createSseParser((...event) => events.push(event))
  const text = ': heartbeat\r\n\r\nevent: delta\r\ndata: {"text":\r\ndata: "中文😊"}\r\n\r\n' + frame('done', { status: 'completed' })
  for (const character of text) parse(character)
  assert.deepEqual(events, [['delta', { text: '中文😊' }], ['done', { status: 'completed' }]])
  assert.throws(() => sse.createSseParser(() => {})('data: broken\n\n'), /无法解析/)
})

test('history contains only completed pairs in order and obeys size and round limits', () => {
  const turns = Array.from({ length: 25 }, (_, i) => ({ question: `q${i}`, answer: `a${i}`, status: 'completed' }))
  turns.push({ question: 'broken', answer: 'partial', status: 'failed' })
  const result = sse.buildHistory(turns, 'next')
  assert.equal(result.length, 40)
  assert.deepEqual(result[0], { role: 'user', content: 'q5' })
  assert.deepEqual(result.at(-1), { role: 'assistant', content: 'a24' })
  assert.deepEqual(sse.buildHistory([{ question: 'q', answer: 'x'.repeat(32001), status: 'completed' }], 'next'), [])
})

test('client sends authenticated JSON and completes only after done; decoder handles split bytes', async () => {
  const bytes = encoder.encode(frame('start', { model: 'test' }) + frame('delta', { text: '中文😊' }) + frame('done', { status: 'completed' }))
  const events = []
  const api = client(async (url, options) => {
    assert.ok(url.endsWith('/api/messages'))
    assert.equal(options.headers.satoken, 'session')
    assert.deepEqual(JSON.parse(options.body), { message: 'hi', effort: 'medium' })
    return new Response(stream([...bytes].map(byte => new Uint8Array([byte]))), { headers: { 'Content-Type': 'text/event-stream' } })
  })
  await api.streamMessage({ message: 'hi', effort: 'medium' }, { onEvent: (...event) => events.push(event) })
  assert.equal(events[1][1].text, '中文😊')
  assert.equal(events.at(-1)[0], 'done')
})

test('client handles null/non-JSON errors and absent content type without TypeError', async () => {
  for (const body of ['null', '<html>unavailable</html>']) {
    await assert.rejects(client(async () => new Response(body, { status: 503 })).streamMessage({}), error => error.status === 503 && /服务/.test(error.message))
  }
  await assert.rejects(client(async () => new Response(null)).streamMessage({}), /未返回流式回答/)
})

test('401 expires the session; 403 keeps it; old-account responses are ignored', async () => {
  for (const status of [401, 403]) {
    const api = client(async () => new Response('{}', { status }))
    await assert.rejects(api.streamMessage({}))
    assert.equal(api.expired.length, status === 401 ? 1 : 0)
  }
  const state = { token: 'old' }
  const api = client(async () => { state.token = 'new'; return new Response('{}', { status: 401 }) }, state)
  await assert.rejects(api.streamMessage({}), /账号已变更/)
  assert.equal(api.expired.length, 0)
})

test('truncated, invalid delta, SSE error and incomplete done all fail', async () => {
  for (const parts of [
    [frame('delta', { text: 'partial' })],
    [frame('delta', { text: null })],
    [frame('error', { message: 'upstream failed' })],
    [frame('done', { status: 'failed' })]
  ]) {
    const api = client(async () => new Response(stream(parts), { headers: { 'Content-Type': 'text/event-stream' } }))
    await assert.rejects(api.streamMessage({}))
  }
})

test('cancel aborts the client request and a late done cannot mark it complete', async () => {
  const controller = new AbortController()
  const api = client(async () => new Response(stream([frame('delta', { text: 'partial' }) + frame('done', { status: 'completed' })]), { headers: { 'Content-Type': 'text/event-stream' } }))
  const events = []
  await assert.rejects(api.streamMessage({}, { signal: controller.signal, onEvent: event => { events.push(event); controller.abort() } }), { name: 'AbortError' })
  assert.deepEqual(events, ['delta'])
})

function conversation(streamMessage) {
  const vue = require('vue')
  const unmount = [], mounted = []
  const store = { state: vue.reactive({ token: 'test' }) }
  const api = load('composables/useAiConversation.js', { vue: { ...vue, onBeforeUnmount: fn => unmount.push(fn), onMounted: fn => mounted.push(fn) }, vuex: { useStore: () => store }, '../api/message': { streamMessage }, '../utils/sse': sse }, { window: { addEventListener() {}, removeEventListener() {} } })
  const scope = vue.effectScope()
  const vm = scope.run(() => api.useAiConversation())
  return { vm, store, close() { unmount.forEach(fn => fn()); scope.stop() } }
}

test('conversation streams reactive answer and sends completed context for the next question', async () => {
  const bodies = []
  const harness = conversation(async (body, { onEvent }) => { bodies.push(body); onEvent('start', { model: 'model' }); onEvent('delta', { text: 'answer' }) })
  const { vm } = harness
  vm.input.value = 'first'; await vm.send()
  assert.equal(vm.turns.value[0].answer, 'answer')
  assert.equal(vm.turns.value[0].status, 'completed')
  assert.equal(vm.activeModel.value, 'model')
  vm.input.value = 'second'; await vm.send()
  assert.deepEqual(bodies[1].history, [{ role: 'user', content: 'first' }, { role: 'assistant', content: 'answer' }])
  harness.close()
})

test('conversation stop preserves partial reply, blocks duplicates, and retries original context', async () => {
  const bodies = []
  let signal
  const harness = conversation(async (body, options) => {
    bodies.push(body); signal = options.signal; options.onEvent('delta', { text: 'partial' })
    await new Promise((resolve, reject) => signal.addEventListener('abort', () => reject(new DOMException('stopped', 'AbortError'))))
  })
  const { vm } = harness
  vm.input.value = 'question'
  const pending = vm.send()
  vm.input.value = 'draft'; vm.send()
  assert.equal(bodies.length, 1)
  vm.stop(); await pending
  assert.equal(vm.turns.value[0].status, 'stopped')
  assert.equal(vm.turns.value[0].answer, 'partial')
  const retry = vm.retry()
  vm.stop(); await retry
  assert.deepEqual(bodies[0], bodies[1])
  harness.close()
})

test('account change and unmount abort generation and clear old-account content', async () => {
  let signal
  const harness = conversation(async (_body, options) => {
    signal = options.signal
    await new Promise((resolve, reject) => signal.addEventListener('abort', () => reject(new DOMException('stopped', 'AbortError'))))
  })
  harness.vm.input.value = 'private'
  const pending = harness.vm.send()
  harness.store.state.token = 'other'
  await pending
  assert.equal(signal.aborted, true)
  assert.equal(harness.vm.turns.value.length, 0)
  assert.equal(harness.vm.input.value, '')
  harness.close()
})
