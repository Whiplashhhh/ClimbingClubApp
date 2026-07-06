<script setup lang="ts">
useHead({ title: 'Créer un compte — Belay' })

const auth = useAuthStore()

const mode = ref<'create' | 'join'>('create')
const email = ref('')
const password = ref('')
const displayName = ref('')
const clubName = ref('')
const climbingType = ref<'BOULDER' | 'ROPES' | 'BOTH'>('BOTH')
const joinSlug = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

const passwordValid = computed(() => passwordChecks(password.value).valid)

async function onSubmit() {
  error.value = null
  loading.value = true
  try {
    await auth.register({
      email: email.value,
      password: password.value,
      displayName: displayName.value,
      ...(mode.value === 'create'
        ? { createOrganization: { name: clubName.value, climbingType: climbingType.value } }
        : { joinSlug: joinSlug.value }),
    })
    await navigateTo('/')
  } catch (e: unknown) {
    const status = (e as { statusCode?: number }).statusCode
    if (status === 409) error.value = 'Cet email est déjà utilisé.'
    else if (status === 404) error.value = 'Aucun club ne correspond à ce code.'
    else if (status === 429) error.value = 'Trop de tentatives. Réessayez plus tard.'
    else error.value = "L'inscription a échoué. Vérifiez les champs saisis."
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto mt-10 max-w-sm">
    <h1 class="mb-6 text-2xl font-bold text-gray-900">Créer un compte</h1>

    <div class="mb-6 grid grid-cols-2 gap-2 rounded-lg bg-gray-100 p-1 text-sm">
      <button
        :class="[
          'rounded-md px-3 py-2',
          mode === 'create' ? 'bg-white font-medium shadow' : 'text-gray-600',
        ]"
        @click="mode = 'create'"
      >
        Créer un club
      </button>
      <button
        :class="[
          'rounded-md px-3 py-2',
          mode === 'join' ? 'bg-white font-medium shadow' : 'text-gray-600',
        ]"
        @click="mode = 'join'"
      >
        Rejoindre un club
      </button>
    </div>

    <form class="flex flex-col gap-4" @submit.prevent="onSubmit">
      <label class="flex flex-col gap-1 text-sm text-gray-700">
        Nom affiché
        <input
          v-model="displayName"
          type="text"
          required
          maxlength="120"
          class="rounded-md border border-gray-300 px-3 py-2"
        >
      </label>
      <label class="flex flex-col gap-1 text-sm text-gray-700">
        Email
        <input
          v-model="email"
          type="email"
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
          required
          minlength="10"
          maxlength="72"
          autocomplete="new-password"
          class="rounded-md border border-gray-300 px-3 py-2"
        >
        <PasswordRules :password="password" />
      </label>

      <template v-if="mode === 'create'">
        <label class="flex flex-col gap-1 text-sm text-gray-700">
          Nom du club
          <input
            v-model="clubName"
            type="text"
            required
            maxlength="120"
            class="rounded-md border border-gray-300 px-3 py-2"
          >
        </label>
        <label class="flex flex-col gap-1 text-sm text-gray-700">
          Type d'escalade
          <select v-model="climbingType" class="rounded-md border border-gray-300 px-3 py-2">
            <option value="BOULDER">Bloc</option>
            <option value="ROPES">Voies</option>
            <option value="BOTH">Bloc et voies</option>
          </select>
        </label>
      </template>
      <label v-else class="flex flex-col gap-1 text-sm text-gray-700">
        Code du club (slug)
        <input
          v-model="joinSlug"
          type="text"
          required
          maxlength="140"
          placeholder="ex. roc-altitude"
          class="rounded-md border border-gray-300 px-3 py-2"
        >
      </label>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      <button
        type="submit"
        :disabled="loading || !passwordValid"
        class="rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {{ mode === 'create' ? 'Créer mon club' : "Demander l'adhésion" }}
      </button>
    </form>
    <p class="mt-4 text-sm text-gray-600">
      Déjà un compte ?
      <NuxtLink to="/login" class="text-indigo-600 hover:underline">Se connecter</NuxtLink>
    </p>
  </div>
</template>
