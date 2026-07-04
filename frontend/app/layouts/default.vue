<script setup lang="ts">
const auth = useAuthStore()

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
