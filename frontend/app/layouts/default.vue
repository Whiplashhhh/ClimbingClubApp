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
      <div class="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
        <div class="flex items-center gap-4">
          <NuxtLink to="/" class="text-xl font-bold text-indigo-600">Belay</NuxtLink>
          <nav v-if="auth.isActive" class="flex items-center gap-3 text-sm">
            <NuxtLink to="/" class="text-gray-600 hover:text-indigo-600">Fil</NuxtLink>
            <NuxtLink to="/members" class="text-gray-600 hover:text-indigo-600">Membres</NuxtLink>
          </nav>
        </div>
        <div v-if="auth.me" class="flex items-center gap-3 text-sm">
          <span class="text-gray-700">{{ auth.me.displayName }}</span>
          <button
            class="rounded-md border border-gray-300 px-3 py-1 text-gray-600 hover:bg-gray-100"
            @click="onLogout"
          >
            Se déconnecter
          </button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-3xl px-4 py-6">
      <slot />
    </main>
  </div>
</template>
