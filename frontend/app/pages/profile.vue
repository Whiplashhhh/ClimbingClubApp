<script setup lang="ts">
useHead({ title: 'Profil — Belay' })

const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  OWNER: 'Président',
  ADMIN: 'Admin',
  COACH: 'Moniteur',
  MEMBER: 'Membre',
}

const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)
const success = ref(false)

const canSubmit = computed(
  () =>
    currentPassword.value.length > 0 &&
    passwordChecks(newPassword.value).valid &&
    newPassword.value === confirmPassword.value,
)

async function changePassword() {
  if (!canSubmit.value) return
  error.value = null
  success.value = false
  submitting.value = true
  try {
    await apiFetch('/api/auth/change-password', {
      method: 'POST',
      body: { currentPassword: currentPassword.value, newPassword: newPassword.value },
    })
    success.value = true
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } catch {
    error.value = 'Le changement a échoué : vérifiez votre mot de passe actuel.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-6">
    <section class="rounded-lg border border-gray-200 bg-white p-4">
      <h1 class="mb-3 text-lg font-semibold text-gray-900">Mon profil</h1>
      <dl class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
        <dt class="text-gray-500">Nom</dt>
        <dd class="text-gray-900">{{ auth.me.displayName }}</dd>
        <dt class="text-gray-500">Email</dt>
        <dd class="text-gray-900">{{ auth.me.email }}</dd>
        <dt class="text-gray-500">Rôle</dt>
        <dd class="text-gray-900">{{ roleLabels[auth.me.role] ?? auth.me.role }}</dd>
        <dt class="text-gray-500">Club</dt>
        <dd class="text-gray-900">
          {{ auth.me.organization.name }}
          <span class="ml-1 rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">
            {{ auth.me.organization.slug }}
          </span>
        </dd>
      </dl>
    </section>

    <form
      class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
      data-testid="change-password"
      @submit.prevent="changePassword"
    >
      <h2 class="text-lg font-semibold text-gray-900">Changer de mot de passe</h2>
      <input
        v-model="currentPassword"
        type="password"
        autocomplete="current-password"
        placeholder="Mot de passe actuel"
        class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
      >
      <input
        v-model="newPassword"
        type="password"
        autocomplete="new-password"
        placeholder="Nouveau mot de passe"
        class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
      >
      <PasswordRules :password="newPassword" />
      <input
        v-model="confirmPassword"
        type="password"
        autocomplete="new-password"
        placeholder="Confirmer le nouveau mot de passe"
        class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
      >
      <p
        v-if="newPassword && confirmPassword && newPassword !== confirmPassword"
        class="text-sm text-amber-600"
      >
        Les deux mots de passe ne correspondent pas.
      </p>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      <p v-if="success" class="text-sm text-green-600">Mot de passe mis à jour ✓</p>
      <button
        type="submit"
        :disabled="submitting || !canSubmit"
        class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {{ submitting ? 'Enregistrement…' : 'Mettre à jour' }}
      </button>
    </form>
  </div>
</template>
