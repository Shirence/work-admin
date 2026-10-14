const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  // 默认 '/' 即可：dev server 与根目录部署都最稳。
  // 只有部署到固定子目录（如 /work-admin/）时才改成 '/work-admin/'。
  publicPath: '/',
  devServer: {
    port: 8080
    // 第 10.2 节：仅在接入真实后端时启用下面的开发代理
    // proxy: {
    //   '/api': {
    //     target: 'http://localhost:3000',
    //     changeOrigin: true,
    //     pathRewrite: { '^/api': '' }
    //   }
    // }
  }
})
