/**
 * ArrayBuffer / Uint8Array <-> Base64 辅助函数。
 * libsignal 的密钥是 ArrayBuffer，服务端传输使用 Base64。
 */
export function arrayBufferToBase64(buffer) {
  const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer)
  let binary = ''
  const chunk = 0x8000
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk))
  }
  return btoa(binary)
}

export function base64ToArrayBuffer(base64) {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

/** libsignal encrypt 返回的 body 是二进制字符串 */
export function binaryStringToBase64(binary) {
  return btoa(binary)
}

export function base64ToBinaryString(base64) {
  return atob(base64)
}
