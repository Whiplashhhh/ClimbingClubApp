<script setup lang="ts">
import { notificationPageSchema, type AppNotification } from '~/schemas/notifications'

useHead({ title: 'Notifications — Belay' })

const PAGE_SIZE = 20

const auth = useAuthStore()
const notificationsStore = useNotificationsStore()

const items = ref<AppNotification[]>([])
const page = ref(0)
const hasNext = ref(false)
const loadError = ref<string | null>(null)

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(iso),
  )
}

async function loadPage(target: number) {
  loadError.value = null
  try {
    const data = notificationPageSchema.parse(
      await apiFetch<unknown>(`/api/notifications?page=${target}&size=${PAGE_SIZE}`),
    )
    items.value = target === 0 ? data.items : [...items.value, ...data.items]
    page.value = data.page
    hasNext.value = data.hasNext
  } catch {
    loadError.value = 'Les notifications n’ont pas pu être chargées.'
  }
}

async function markAllRead() {
  try {
    await apiFetch('/api/notifications/read-all', { method: 'POST' })
    notificationsStore.markAllReadLocally()
  } catch {
    // non bloquant : les items restent affichés comme non lus
  }
}

async function refresh() {
  if (auth.isActive) {
    await loadPage(0)
    // Ouvrir la page (ou la rafraîchir) vaut lecture : les items affichés gardent leur
    // état visuel « non lu »
    await markAllRead()
  }
}

await useAsyncData('notifications', async () => {
  await refresh()
  return true
})

useAutoRefresh(refresh)
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-4">
    <div class="flex items-center justify-between gap-2">
      <h1 class="text-xl font-bold text-gray-900">Notifications</h1>
      <button
        type="button"
        class="shrink-0 rounded-md border border-gray-300 p-2 text-gray-500 hover:bg-gray-100"
        aria-label="Actualiser les notifications"
        @click="refresh"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          class="h-4 w-4"
        >
          <path d="M21 12a9 9 0 1 1-2.64-6.36L21 8" />
          <path d="M21 3v5h-5" />
        </svg>
      </button>
    </div>

    <p v-if="loadError" class="text-sm text-red-600">{{ loadError }}</p>

    <section class="flex flex-col gap-2" data-testid="notifications-list">
      <p v-if="items.length === 0 && !loadError" class="text-sm text-gray-500">
        Aucune notification.
      </p>
      <article
        v-for="notification in items"
        :key="notification.id"
        class="rounded-lg border bg-white p-3"
        :class="notification.readAt ? 'border-gray-200' : 'border-indigo-300 bg-indigo-50'"
      >
        <p class="text-sm text-gray-800">{{ notification.message }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ formatDate(notification.createdAt) }}</p>
      </article>
    </section>

    <button
      v-if="hasNext"
      type="button"
      class="self-center rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-100"
      @click="loadPage(page + 1)"
    >
      Voir plus
    </button>
  </div>
</template>
