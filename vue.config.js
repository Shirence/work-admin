const { defineConfig } = require('@vue/cli-service')
const backend = process.env.VUE_APP_BACKEND_TARGET || 'http://118.25.25.164:8080'
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8083,
    proxy: {
      '/api': {
        target: backend,
        changeOrigin: true,
        headers: { Origin: new URL(backend).origin },
        timeout: 240000,
        proxyTimeout: 240000
      },
      '/ws/chat': {
        target: backend,
        ws: true,
        changeOrigin: true,
        headers: { Origin: new URL(backend).origin }
      }
    }
  }
})
