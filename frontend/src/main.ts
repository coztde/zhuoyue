import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useGlobalNotice } from '@/composables/useGlobalNotice'
import './style.css'

const app = createApp(App)
const { showNotice } = useGlobalNotice()

app.config.errorHandler = (error) => {
  console.error('Vue 运行时异常', error)
  showNotice('页面出了点问题，请刷新后重试', 'error')
}

window.addEventListener('error', (event) => {
  console.error('全局脚本异常', event.error ?? event.message)
  showNotice('页面运行异常，请刷新后重试', 'error')
})

window.addEventListener('unhandledrejection', (event) => {
  const reason = event.reason instanceof Error ? event.reason.message : '发生了未处理的异步异常'
  console.error('未处理的 Promise 异常', event.reason)
  showNotice(reason, 'error')
})

app.use(createPinia()).use(router).mount('#app')
