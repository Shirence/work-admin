import {
  KeyHelper,
  SignalProtocolAddress,
  SessionBuilder,
  SessionCipher
} from '@privacyresearch/libsignal-protocol-typescript'
import { registerChatDevice, getChatDevices } from '../api/chat'
import { LocalSignalStore } from './signalStore'
import {
  arrayBufferToBase64,
  base64ToArrayBuffer,
  binaryStringToBase64,
  base64ToBinaryString
} from './bytes'

const LOCAL_DEVICE_PREFIX = 'wa-local-device:'
const PENDING_PREFIX = 'wa-pending-device:'

function readJson(key) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

function writeJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

export function getStoredDevice(username) {
  return readJson(`${LOCAL_DEVICE_PREFIX}${username}`)
}

export function storeDevice(device) {
  writeJson(`${LOCAL_DEVICE_PREFIX}${device.username}`, device)
}

export function clearStoredDevice(username) {
  localStorage.removeItem(`${LOCAL_DEVICE_PREFIX}${username}`)
  localStorage.removeItem(`${PENDING_PREFIX}${username}`)
}

export function signalStoreFor(clientDeviceKey) {
  return new LocalSignalStore(clientDeviceKey)
}

function randomDeviceKey() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return `dev-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

function deviceName() {
  const platform = (navigator.platform || 'Web').slice(0, 40)
  return `Web · ${platform}`
}

/**
 * 首次注册：生成身份密钥、签名预密钥、一次性预密钥，并只把公钥发给服务端。
 */
async function generateRegistration(username) {
  const clientDeviceKey = randomDeviceKey()
  const store = signalStoreFor(clientDeviceKey)
  const identityKeyPair = await KeyHelper.generateIdentityKeyPair()
  const registrationId = KeyHelper.generateRegistrationId()
  const signedPreKeyId = 1
  const signedPreKey = await KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId)

  store.setIdentityKeyPair(identityKeyPair)
  store.setRegistrationId(registrationId)
  store.storeSignedPreKey(signedPreKeyId, signedPreKey.keyPair)
  store.setSignedPreKeyMeta({
    keyId: signedPreKeyId,
    signature: arrayBufferToBase64(signedPreKey.signature)
  })

  const oneTimePreKeys = []
  for (let keyId = 1; keyId <= 10; keyId++) {
    const preKey = await KeyHelper.generatePreKey(keyId)
    store.storePreKey(keyId, preKey.keyPair)
    oneTimePreKeys.push({
      keyId: preKey.keyId,
      publicKey: arrayBufferToBase64(preKey.keyPair.pubKey)
    })
  }

  const payload = {
    clientDeviceKey,
    deviceName: deviceName(),
    registrationId,
    identityKey: arrayBufferToBase64(identityKeyPair.pubKey),
    signedPreKeyId,
    signedPreKeyPublic: arrayBufferToBase64(signedPreKey.keyPair.pubKey),
    signedPreKeySignature: arrayBufferToBase64(signedPreKey.signature),
    oneTimePreKeys
  }
  writeJson(`${PENDING_PREFIX}${username}`, { username, payload, createdAt: Date.now() })
  return payload
}

/**
 * 确保当前账号有一台已注册设备。
 * - 已有 ACTIVE/PENDING 设备：直接返回本地信息；
 * - 注册中断（REGISTERING）：复用已生成的公钥重试；
 * - 否则生成新密钥并注册。
 */
export async function ensureDevice(username) {
  const existing = getStoredDevice(username)
  if (existing && existing.status !== 'REGISTERING') {
    return {
      device: existing,
      store: signalStoreFor(existing.clientDeviceKey),
      pendingApproval: existing.status === 'PENDING'
    }
  }

  let pending = readJson(`${PENDING_PREFIX}${username}`)
  if (!pending || !pending.payload || pending.payload.clientDeviceKey !== existing?.clientDeviceKey) {
    if (existing && existing.status === 'REGISTERING') {
      // 旧的重试数据不完整，重新生成
      localStorage.removeItem(`${PENDING_PREFIX}${username}`)
    }
    const payload = await generateRegistration(username)
    pending = readJson(`${PENDING_PREFIX}${username}`)
    if (!payload) throw new Error('生成设备密钥失败')
  }

  const res = await registerChatDevice(pending.payload)
  const remote = res.data.device
  const local = {
    clientDeviceKey: remote.clientDeviceKey,
    username,
    deviceId: remote.deviceId,
    status: remote.status,
    deviceName: remote.deviceName,
    registrationId: remote.registrationId,
    linkId: res.data.linkId,
    createdAt: Date.now()
  }
  storeDevice(local)
  localStorage.removeItem(`${PENDING_PREFIX}${username}`)
  return {
    device: local,
    store: signalStoreFor(local.clientDeviceKey),
    pendingApproval: Boolean(res.data.pendingApproval)
  }
}

/**
 * 从服务端刷新本地设备状态（例如另一台设备批准后，PENDING -> ACTIVE）。
 */
export async function refreshDeviceStatus(username, localDevice) {
  const res = await getChatDevices()
  const remote = (res.data || []).find(d => d.clientDeviceKey === localDevice.clientDeviceKey)
  if (!remote) {
    clearStoredDevice(username)
    return { status: 'MISSING', device: null }
  }
  const updated = {
    ...localDevice,
    deviceId: remote.deviceId,
    status: remote.status,
    deviceName: remote.deviceName,
    approvedAt: remote.approvedAt
  }
  storeDevice(updated)
  return { status: remote.status, device: updated }
}

function toDeviceType(bundle) {
  return {
    identityKey: base64ToArrayBuffer(bundle.identityKey),
    signedPreKey: {
      keyId: bundle.signedPreKeyId,
      publicKey: base64ToArrayBuffer(bundle.signedPreKeyPublic),
      signature: base64ToArrayBuffer(bundle.signedPreKeySignature)
    },
    preKey: bundle.oneTimePreKey
      ? {
          keyId: bundle.oneTimePreKey.keyId,
          publicKey: base64ToArrayBuffer(bundle.oneTimePreKey.publicKey)
        }
      : undefined,
    registrationId: bundle.registrationId
  }
}

/**
 * 对每个目标设备分别建立/复用 libsignal 会话并加密。
 * 返回服务端可直接接收的 envelopes 数组。
 */
export async function encryptForDeviceBundles(store, plaintext, bundles) {
  const result = []
  const seen = new Set()
  for (const bundle of bundles) {
    if (!bundle || !bundle.clientDeviceKey || seen.has(bundle.clientDeviceKey)) continue
    seen.add(bundle.clientDeviceKey)

    const address = new SignalProtocolAddress(bundle.username, bundle.deviceId)
    let cipher = new SessionCipher(store, address)
    let hasSession = false
    try {
      hasSession = await cipher.hasOpenSession()
    } catch (e) {
      hasSession = false
    }
    if (!hasSession) {
      await new SessionBuilder(store, address).processPreKey(toDeviceType(bundle))
      cipher = new SessionCipher(store, address)
    }

    const plaintextBuffer = new TextEncoder().encode(plaintext).buffer
    const encrypted = await cipher.encrypt(plaintextBuffer)
    result.push({
      targetDeviceKey: bundle.clientDeviceKey,
      cipherType: encrypted.type,
      body: binaryStringToBase64(encrypted.body)
    })
  }
  return result
}

/**
 * 解密发给当前设备的信封。调用方需按 serverSeq 顺序逐条调用。
 */
export async function decryptEnvelope(store, message) {
  const envelope = message.envelope
  if (!envelope) {
    throw new Error('没有发给当前设备的密文')
  }
  const address = new SignalProtocolAddress(message.senderUsername, message.senderDeviceId)
  const cipher = new SessionCipher(store, address)
  const body = base64ToBinaryString(envelope.body)
  const plaintext = Number(envelope.cipherType) === 3
    ? await cipher.decryptPreKeyWhisperMessage(body, 'binary')
    : await cipher.decryptWhisperMessage(body, 'binary')
  return new TextDecoder().decode(new Uint8Array(plaintext))
}
