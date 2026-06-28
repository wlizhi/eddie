import {createApp} from 'vue'
import {createPinia} from 'pinia'
import router from './router'
import App from './App.vue'
import './assets/styles/markdown.css'
import './assets/styles/theme.css'
import './assets/styles/app-layout.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
