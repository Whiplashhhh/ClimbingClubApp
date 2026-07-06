<script setup lang="ts">
useHead({ title: 'Profil — Belay' })

const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  OWNER: 'Président',
  ADMIN: 'Admin',
  COACH: 'Moniteur',
  MEMBER: 'Membre',
}

// Photo de profil
const avatarError = ref<string | null>(null)
const avatarUploading = ref(false)
const avatarInput = ref<HTMLInputElement | null>(null)

const avatarInitials = computed(() => {
  const parts = (auth.me?.displayName ?? '').trim().split(/\s+/)
  return parts
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join('')
})

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  avatarError.value = null
  avatarUploading.value = true
  try {
    const form = new FormData()
    form.append('image', file)
    await apiFetch('/api/auth/avatar', { method: 'POST', body: form })
    await auth.fetchMe()
  } catch {
    avatarError.value = 'Envoi de la photo échoué (JPEG/PNG/WebP, 5 Mo max).'
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

// Édition des infos (nom + email)
const editName = ref('')
const editEmail = ref('')
const editPassword = ref('')
const editError = ref<string | null>(null)
const editSuccess = ref(false)
const editSubmitting = ref(false)

watch(
  () => auth.me,
  (me) => {
    if (me) {
      editName.value = me.displayName
      editEmail.value = me.email
    }
  },
  { immediate: true },
)

const canSaveInfo = computed(
  () => editPassword.value.length > 0 && (editName.value.trim().length > 0 || editEmail.value.trim().length > 0),
)

async function saveInfo() {
  if (!canSaveInfo.value) return
  editError.value = null
  editSuccess.value = false
  editSubmitting.value = true
  try {
    await apiFetch('/api/auth/profile', {
      method: 'PATCH',
      body: {
        displayName: editName.value.trim() || undefined,
        email: editEmail.value.trim() || undefined,
        currentPassword: editPassword.value,
      },
    })
    await auth.fetchMe()
    editPassword.value = ''
    editSuccess.value = true
  } catch (e: unknown) {
    const status = (e as { statusCode?: number }).statusCode
    editError.value =
      status === 409 ? 'Cet email est déjà utilisé.' : 'Échec : vérifiez votre mot de passe actuel.'
  } finally {
    editSubmitting.value = false
  }
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
      <div class="mb-4 flex items-center gap-4">
        <div
          class="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-full bg-indigo-600 text-lg font-semibold text-white"
        >
          <img
            v-if="auth.me.avatarUrl"
            :src="auth.me.avatarUrl"
            alt="Photo de profil"
            class="h-full w-full object-cover"
          >
          <span v-else>{{ avatarInitials }}</span>
        </div>
        <div class="flex flex-col gap-1">
          <label
            class="cursor-pointer self-start rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
          >
            {{ avatarUploading ? 'Envoi…' : 'Changer la photo' }}
            <input
              ref="avatarInput"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              class="hidden"
              data-testid="avatar-input"
              @change="uploadAvatar"
            >
          </label>
          <p v-if="avatarError" class="text-sm text-red-600">{{ avatarError }}</p>
        </div>
      </div>
      <dl class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
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
      data-testid="edit-info"
      @submit.prevent="saveInfo"
    >
      <h2 class="text-lg font-semibold text-gray-900">Mes informations</h2>
      <label class="flex flex-col gap-1 text-sm text-gray-600">
        Nom affiché
        <input
          v-model="editName"
          type="text"
          maxlength="120"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
      </label>
      <label class="flex flex-col gap-1 text-sm text-gray-600">
        Email
        <input
          v-model="editEmail"
          type="email"
          autocomplete="email"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
      </label>
      <label class="flex flex-col gap-1 text-sm text-gray-600">
        Mot de passe actuel (pour confirmer)
        <input
          v-model="editPassword"
          type="password"
          autocomplete="current-password"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
      </label>
      <p v-if="editError" class="text-sm text-red-600">{{ editError }}</p>
      <p v-if="editSuccess" class="text-sm text-green-600">Informations mises à jour ✓</p>
      <button
        type="submit"
        :disabled="editSubmitting || !canSaveInfo"
        class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
      >
        {{ editSubmitting ? 'Enregistrement…' : 'Enregistrer' }}
      </button>
    </form>

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
