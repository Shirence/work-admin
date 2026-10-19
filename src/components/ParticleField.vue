<template>
  <canvas ref="cv" class="particle-field" aria-hidden="true"></canvas>
</template>

<script>
/**
 * 极轻的环境粒子场：给白底加一点空间感，只做氛围，不抢主体。
 * 系统开启「减少动效」时保持静止。
 */
export default {
  name: 'ParticleField',
  data() {
    return { raf: 0 }
  },
  mounted() {
    this.prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    this.canvas = this.$refs.cv
    this.ctx = this.canvas.getContext('2d')
    this.resize()
    window.addEventListener('resize', this.resize)
    if (!this.prefersReduced) {
      this.raf = requestAnimationFrame(this.loop)
    } else {
      this.draw()
    }
  },
  beforeUnmount() {
    cancelAnimationFrame(this.raf)
    window.removeEventListener('resize', this.resize)
  },
  methods: {
    resize() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      const w = this.canvas.clientWidth || 1
      const h = this.canvas.clientHeight || 1
      this.canvas.width = w * dpr
      this.canvas.height = h * dpr
      this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      this.w = w
      this.h = h
      this.build()
      if (this.prefersReduced) this.draw()
    },
    build() {
      const count = Math.min(58, Math.round((this.w * this.h) / 26000))
      this.dots = Array.from({ length: count }, (_, i) => ({
        x: Math.random() * this.w,
        y: Math.random() * this.h,
        vx: (Math.random() - 0.5) * 0.14,
        vy: (Math.random() - 0.5) * 0.14,
        r: Math.random() * 1.6 + 0.7,
        magenta: i % 9 === 0
      }))
    },
    loop() {
      this.dots.forEach(d => {
        d.x += d.vx
        d.y += d.vy
        if (d.x < -20) d.x = this.w + 20
        if (d.x > this.w + 20) d.x = -20
        if (d.y < -20) d.y = this.h + 20
        if (d.y > this.h + 20) d.y = -20
      })
      this.draw()
      this.raf = requestAnimationFrame(this.loop)
    },
    draw() {
      const ctx = this.ctx
      const dots = this.dots || []
      ctx.clearRect(0, 0, this.w, this.h)
      for (let i = 0; i < dots.length; i++) {
        for (let j = i + 1; j < dots.length; j++) {
          const dx = dots[i].x - dots[j].x
          const dy = dots[i].y - dots[j].y
          const dist = Math.sqrt(dx * dx + dy * dy)
          if (dist < 132) {
            ctx.strokeStyle = 'rgba(94,242,214,' + (0.07 * (1 - dist / 132)).toFixed(3) + ')'
            ctx.lineWidth = 1
            ctx.beginPath()
            ctx.moveTo(dots[i].x, dots[i].y)
            ctx.lineTo(dots[j].x, dots[j].y)
            ctx.stroke()
          }
        }
      }
      dots.forEach(d => {
        ctx.fillStyle = d.magenta ? 'rgba(255,61,129,.30)' : 'rgba(94,242,214,.22)'
        ctx.beginPath()
        ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
        ctx.fill()
      })
    }
  }
}
</script>

<style scoped>
.particle-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  pointer-events: none;
}
</style>
