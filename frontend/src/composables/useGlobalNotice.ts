import { ref } from 'vue'

type NoticeType = 'info' | 'success' | 'error'

const visible = ref(false)
const message = ref('')
const type = ref<NoticeType>('info')
let timer: number | null = null

export function useGlobalNotice() {
  const showNotice = (nextMessage: string, nextType: NoticeType = 'info', duration = 3200) => {
    message.value = nextMessage
    type.value = nextType
    visible.value = true

    if (timer) {
      window.clearTimeout(timer)
    }

    timer = window.setTimeout(() => {
      visible.value = false
    }, duration)
  }

  const hideNotice = () => {
    visible.value = false
    if (timer) {
      window.clearTimeout(timer)
      timer = null
    }
  }

  return {
    visible,
    message,
    type,
    showNotice,
    hideNotice,
  }
}
