<script setup lang="ts">
const auth = useAuthStore()
const notificationsStore = useNotificationsStore()
const route = useRoute()

// Pastille rafraîchie à l'arrivée et à chaque navigation (pas encore de push — Phase 7)
watch(
  () => [auth.isActive, route.path],
  () => {
    if (auth.isActive) notificationsStore.refreshUnreadCount()
  },
  { immediate: true },
)

async function onLogout() {
  await auth.logout()
  await navigateTo('/login')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <header class="border-b border-gray-200 bg-white">
      <div class="mx-auto max-w-3xl px-4 py-3">
        <!-- Rangée 1 : marque + compte. min-w-0/truncate : le bouton reste visible sur mobile -->
        <div class="flex items-center justify-between gap-3">
          <NuxtLink to="/" class="shrink-0 text-xl font-bold text-indigo-600">Belay</NuxtLink>
          <div v-if="auth.me" class="flex min-w-0 items-center gap-3 text-sm">
            <NuxtLink
              v-if="auth.isActive"
              to="/notifications"
              class="relative shrink-0 text-gray-500 hover:text-indigo-600"
              aria-label="Notifications"
              data-testid="bell"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="h-5 w-5"
              >
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
              </svg>
              <span
                v-if="notificationsStore.unreadCount > 0"
                class="absolute -top-1.5 -right-1.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white"
                data-testid="bell-badge"
              >
                {{ notificationsStore.unreadCount > 9 ? '9+' : notificationsStore.unreadCount }}
              </span>
            </NuxtLink>
            <span class="truncate text-gray-700">{{ auth.me.displayName }}</span>
            <button
              class="shrink-0 rounded-md border border-gray-300 px-3 py-1 text-gray-600 hover:bg-gray-100"
              @click="onLogout"
            >
              Se déconnecter
            </button>
          </div>
        </div>
        <!-- Rangée 2 : navigation, sur sa propre ligne pour rester lisible sur téléphone -->
        <nav v-if="auth.isActive" class="mt-2 flex gap-5 text-sm">
          <NuxtLink
            to="/"
            class="text-gray-600 hover:text-indigo-600"
            exact-active-class="font-semibold text-indigo-600"
          >
            Fil
          </NuxtLink>
          <NuxtLink
            to="/slots"
            class="text-gray-600 hover:text-indigo-600"
            exact-active-class="font-semibold text-indigo-600"
          >
            Créneaux
          </NuxtLink>
          <NuxtLink
            to="/members"
            class="text-gray-600 hover:text-indigo-600"
            exact-active-class="font-semibold text-indigo-600"
          >
            Membres
          </NuxtLink>
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-3xl px-4 py-6">
      <slot />
    </main>
  </div>
</template>
