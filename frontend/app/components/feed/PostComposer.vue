<script setup lang="ts">
import { feedPostSchema, type PostAudience, type PostType } from '~/schemas/feed'

const emit = defineEmits<{ published: [] }>()

const auth = useAuthStore()

// L'audience découle du rôle (le serveur re-vérifie) : admins → org entière, moniteur → ses élèves
const audience = computed<PostAudience>(() => (auth.isAdmin ? 'ORG' : 'COACH_STUDENTS'))
const audienceLabel = computed(() =>
  audience.value === 'ORG' ? 'Visible par tout le club' : 'Visible par vos élèves',
)

const type = ref<PostType>('INFO')
const title = ref('')
const body = ref('')
const imageFile = ref<File | null>(null)
const submitting = ref(false)
const error = ref<string | null>(null)

const typeOptions: { value: PostType; label: string }[] = [
  { value: 'INFO', label: 'Info' },
  { value: 'CANCELLATION', label: 'Cours annulé' },
  { value: 'POSTER', label: 'Affiche' },
]

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  imageFile.value = input.files?.[0] ?? null
}

async function submit() {
  error.value = null
  submitting.value = true
  try {
    if (type.value === 'POSTER') {
      if (!imageFile.value) {
        error.value = 'Choisissez une image pour l’affiche.'
        return
      }
      const meta = { audience: audience.value, title: title.value, body: body.value || undefined }
      const form = new FormData()
      form.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
      form.append('image', imageFile.value)
      feedPostSchema.parse(
        await apiFetch<unknown>('/api/posts/poster', { method: 'POST', body: form }),
      )
    } else {
      feedPostSchema.parse(
        await apiFetch<unknown>('/api/posts', {
          method: 'POST',
          body: {
            type: type.value,
            audience: audience.value,
            title: title.value,
            body: body.value || undefined,
          },
        }),
      )
    }
    title.value = ''
    body.value = ''
    imageFile.value = null
    emit('published')
  } catch {
    error.value = 'La publication a échoué. Vérifiez le formulaire (image JPEG/PNG/WebP, 5 Mo max).'
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

    <div class="flex gap-2">
      <button
        v-for="option in typeOptions"
        :key="option.value"
        type="button"
        class="rounded-md border px-3 py-1 text-sm"
        :class="
          type === option.value
            ? 'border-indigo-600 bg-indigo-600 text-white'
            : 'border-gray-300 text-gray-600 hover:bg-gray-100'
        "
        @click="type = option.value"
      >
        {{ option.label }}
      </button>
    </div>

    <input
      v-model="title"
      type="text"
      required
      maxlength="200"
      placeholder="Titre"
      class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
    >
    <textarea
      v-model="body"
      rows="3"
      maxlength="5000"
      placeholder="Message (optionnel)"
      class="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
    />
    <input
      v-if="type === 'POSTER'"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      class="text-sm text-gray-600"
      @change="onFileChange"
    >

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
