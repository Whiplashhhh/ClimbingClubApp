<script setup lang="ts">
useHead({ title: 'Connexion — Belay' })

const auth = useAuthStore()
const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

async function onSubmit() {
  error.value = null
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    await navigateTo('/')
  } catch (e: unknown) {
    const status = (e as { statusCode?: number }).statusCode
    error.value =
      status === 429
        ? 'Trop de tentatives. Réessayez dans quelques minutes.'
        : 'Email ou mot de passe incorrect.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto mt-10 max-w-sm">
    <h1 class="mb-6 text-2xl font-bold text-gray-900">Connexion</h1>
    <form class="flex flex-col gap-4" @submit.prevent="onSubmit">
      <label class="flex flex-col gap-1 text-sm text-gray-700">
        Email
        <input
          v-model="email"
          type="email"
          name="email"
          required
          autocomplete="email"
          class="rounded-md border border-gray-300 px-3 py-2"
        >
      </label>
      <label class="flex flex-col gap-1 text-sm text-gray-700">
        Mot de passe
        <input
          v-model="password"
          type="password"
          name="password"
          required
          autocomplete="current-password"
          class="rounded-md border border-gray-300 px-3 py-2"
        >
      </label>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      <button
        type="submit"
        :disabled="loading"
        class="rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        Se connecter
      </button>
    </form>
    <p class="mt-4 text-sm text-gray-600">
      Pas encore de compte ?
      <NuxtLink to="/register" class="text-indigo-600 hover:underline">Créer un compte</NuxtLink>
    </p>
  </div>
</template>
