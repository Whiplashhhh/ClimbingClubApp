<script setup lang="ts">
import { feedPostSchema, type PostAudience } from '~/schemas/feed'

const MAX_IMAGES = 4

const emit = defineEmits<{ published: [] }>()

const auth = useAuthStore()

// L'audience découle du rôle (le serveur re-vérifie) : admins → org entière, moniteur → ses élèves
const audience = computed<PostAudience>(() => (auth.isAdmin ? 'ORG' : 'COACH_STUDENTS'))
const audienceLabel = computed(() =>
  audience.value === 'ORG' ? 'Visible par tout le club' : 'Visible par vos élèves',
)

const title = ref('')
const body = ref('')
const imageFiles = ref<File[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const error = ref<string | null>(null)

function onFilesChange(event: Event) {
  const input = event.target as HTMLInputElement
  imageFiles.value = Array.from(input.files ?? []).slice(0, MAX_IMAGES)
}

async function submit() {
  error.value = null
  submitting.value = true
  try {
    const meta = { audience: audience.value, title: title.value, body: body.value || undefined }
    const form = new FormData()
    form.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
    for (const file of imageFiles.value) {
      form.append('images', file)
    }
    feedPostSchema.parse(await apiFetch<unknown>('/api/posts', { method: 'POST', body: form }))
    title.value = ''
    body.value = ''
    imageFiles.value = []
    if (fileInput.value) fileInput.value.value = ''
    emit('published')
  } catch {
    error.value =
      'La publication a échoué. Vérifiez le formulaire (images JPEG/PNG/WebP, 5 Mo max chacune).'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form
    class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
    data-testid="post-composer"
    @submit.prevent="submit"
  >
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">Publier</h2>
      <span class="rounded bg-indigo-50 px-2 py-0.5 text-xs text-indigo-700">
        {{ audienceLabel }}
      </span>
    </div>

    <input
      v-model="title"
      type="text"
      required
      maxlength="200"
      placeholder="Titre"
      class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
    >
    <textarea
      v-model="body"
      rows="3"
      maxlength="5000"
      placeholder="Message (optionnel)"
      class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
    />
    <label class="flex flex-col gap-1 text-sm text-gray-600">
      Images (optionnel, {{ MAX_IMAGES }} max)
      <input
        ref="fileInput"
        type="file"
        multiple
        accept="image/jpeg,image/png,image/webp"
        class="text-gray-600"
        @change="onFilesChange"
      >
    </label>
    <p v-if="imageFiles.length > 0" class="text-xs text-gray-500">
      {{ imageFiles.length }} image{{ imageFiles.length > 1 ? 's' : '' }} sélectionnée{{
        imageFiles.length > 1 ? 's' : ''
      }}
    </p>

    <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

    <button
      type="submit"
      :disabled="submitting || !title"
      class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
    >
      {{ submitting ? 'Publication…' : 'Publier' }}
    </button>
  </form>
</template>
