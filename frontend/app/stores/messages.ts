import { defineStore } from 'pinia'
import { z } from 'zod'
import { conversationSchema } from '~/schemas/messages'

/** Total de messages non lus (pastille de l'icône Messages dans l'en-tête). */
export const useMessagesStore = defineStore('messages', () => {
  const unreadCount = ref(0)

  async function refreshUnreadCount(): Promise<void> {
    try {
      const conversations = z
        .array(conversationSchema)
        .parse(await apiFetch<unknown>('/api/conversations'))
      unreadCount.value = conversations.reduce((sum, c) => sum + c.unread, 0)
    } catch {
      // non bloquant : la pastille reste telle quelle
    }
  }

  return { unreadCount, refreshUnreadCount }
})
