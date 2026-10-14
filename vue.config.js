const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  // 默认 '/' 即可：dev server 与根目录部署都最稳。
  // 只有部署到固定子目录（如 /work-admin/）时才改成 '/work-admin/'。
  publicPath: '/',
  devServer: {
    port: 8080,
    // 开发代理：把 /api 转发到 Spring Boot（8081）。
    // 后端接口路径本身就带 /api 前缀，所以不要写 pathRewrite。
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
