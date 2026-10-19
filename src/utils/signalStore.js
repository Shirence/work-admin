import { arrayBufferToBase64, base64ToArrayBuffer } from './bytes'

const PREFIX = 'wa-signal:'

function readJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : fallback
  } catch (e) {
    return fallback
  }
}

function writeJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

function encodeKeyPair(keyPair) {
  if (!keyPair) return null
  return {
    pubKey: arrayBufferToBase64(keyPair.pubKey),
    privKey: arrayBufferToBase64(keyPair.privKey)
  }
}

function decodeKeyPair(value) {
  if (!value) return undefined
  return {
    pubKey: base64ToArrayBuffer(value.pubKey),
    privKey: base64ToArrayBuffer(value.privKey)
  }
}

/**
 * libsignal StorageType 的 localStorage 实现。
 * 每个 clientDeviceKey 一个独立命名空间，不同账号/设备互不影响。
 */
export class LocalSignalStore {
  constructor(clientDeviceKey) {
    this.clientDeviceKey = clientDeviceKey
    this.prefix = `${PREFIX}${clientDeviceKey}:`
  }

  key(name) {
    return `${this.prefix}${name}`
  }

  get(name, fallback = null) {
    return readJson(this.key(name), fallback)
  }

  set(name, value) {
    writeJson(this.key(name), value)
  }

  setIdentityKeyPair(keyPair) {
    this.set('identityKeyPair', encodeKeyPair(keyPair))
  }

  setRegistrationId(registrationId) {
    this.set('registrationId', registrationId)
  }

  setSignedPreKeyMeta(meta) {
    this.set('signedPreKeyMeta', meta)
  }

  getSignedPreKeyMeta() {
    return this.get('signedPreKeyMeta')
  }

  async getIdentityKeyPair() {
    return decodeKeyPair(this.get('identityKeyPair'))
  }

  async getLocalRegistrationId() {
    return this.get('registrationId')
  }

  async isTrustedIdentity(identifier, identityKey, direction) {
    const identities = this.get('identities', {})
    const stored = identities[identifier]
    if (!stored) return true
    return stored === arrayBufferToBase64(identityKey)
  }

  async saveIdentity(encodedAddress, publicKey, nonblockingApproval) {
    const identities = this.get('identities', {})
    const key = arrayBufferToBase64(publicKey)
    const previous = identities[encodedAddress]
    identities[encodedAddress] = key
    this.set('identities', identities)
    return previous !== undefined && previous !== key
  }

  async loadPreKey(keyId) {
    const preKeys = this.get('preKeys', {})
    return decodeKeyPair(preKeys[String(keyId)])
  }

  async storePreKey(keyId, keyPair) {
    const preKeys = this.get('preKeys', {})
    preKeys[String(keyId)] = encodeKeyPair(keyPair)
    this.set('preKeys', preKeys)
  }

  async removePreKey(keyId) {
    const preKeys = this.get('preKeys', {})
    delete preKeys[String(keyId)]
    this.set('preKeys', preKeys)
  }

  async loadSignedPreKey(keyId) {
    const keys = this.get('signedPreKeys', {})
    return decodeKeyPair(keys[String(keyId)])
  }

  async storeSignedPreKey(keyId, keyPair) {
    const keys = this.get('signedPreKeys', {})
    keys[String(keyId)] = encodeKeyPair(keyPair)
    this.set('signedPreKeys', keys)
  }

  async removeSignedPreKey(keyId) {
    const keys = this.get('signedPreKeys', {})
    delete keys[String(keyId)]
    this.set('signedPreKeys', keys)
  }

  async storeSession(encodedAddress, record) {
    const sessions = this.get('sessions', {})
    sessions[encodedAddress] = record
    this.set('sessions', sessions)
  }

  async loadSession(encodedAddress) {
    const sessions = this.get('sessions', {})
    return sessions[encodedAddress]
  }
}
