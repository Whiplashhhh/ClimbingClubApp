import { defineStore } from 'pinia'
import { notificationPageSchema } from '~/schemas/notifications'

/** Compteur de notifications non lues (pastille de la cloche). */
export const useNotificationsStore = defineStore('notifications', () => {
  const unreadCount = ref(0)

  async function refreshUnreadCount(): Promise<void> {
    try {
      const page = notificationPageSchema.parse(
        await apiFetch<unknown>('/api/notifications?page=0&size=1'),
      )
      unreadCount.value = page.unreadCount
    } catch {
      // non bloquant : la pastille reste telle quelle
    }
  }

  function markAllReadLocally(): void {
    unreadCount.value = 0
  }

  return { unreadCount, refreshUnreadCount, markAllReadLocally }
})
