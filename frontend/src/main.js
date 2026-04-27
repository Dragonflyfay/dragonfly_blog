import './assets/main.scss'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'

import 'element-plus/dist/index.css'

import router from '@/router'

import App from './App.vue'

import { createPinia } from 'pinia'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
