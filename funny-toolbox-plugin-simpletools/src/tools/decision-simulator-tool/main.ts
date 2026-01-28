import { createApp } from 'vue'
import DecisionSimulatorApp from './DecisionSimulatorApp.vue'
import '@/style.css'
import { MotionPlugin } from '@vueuse/motion'
import { Toaster } from '@/components/ui/sonner'

const app = createApp(DecisionSimulatorApp)
app.use(MotionPlugin)
app.component('Toaster', Toaster)
app.mount('#app')
