<script setup lang="ts">
import { pollSchema, type CreatePollPayload } from '~/schemas/polls'
import type { PostAudience } from '~/schemas/feed'

const MAX_OPTIONS = 10

const emit = defineEmits<{ created: [] }>()

const auth = useAuthStore()

// L'audience découle du rôle (le serveur re-vérifie) : admins → org entière, moniteur → ses élèves
const audience = computed<PostAudience>(() => (auth.isAdmin ? 'ORG' : 'COACH_STUDENTS'))
const audienceLabel = computed(() =>
  audience.value === 'ORG' ? 'Visible par tout le club' : 'Visible par vos élèves',
)

const question = ref('')
const options = ref<string[]>(['', ''])
const closesAt = ref('')
const submitting = ref(false)
const error = ref<string | null>(null)

const filledOptions = computed(() => options.value.map((o) => o.trim()).filter((o) => o.length > 0))
const canSubmit = computed(() => question.value.trim().length > 0 && filledOptions.value.length >= 2)

function addOption() {
  if (options.value.length < MAX_OPTIONS) options.value.push('')
}
function removeOption(index: number) {
  if (options.value.length > 2) options.value.splice(index, 1)
}

async function submit() {
  if (!canSubmit.value) return
  error.value = null
  submitting.value = true
  try {
    const payload: CreatePollPayload = {
      audience: audience.value,
      question: question.value.trim(),
      options: filledOptions.value,
      // datetime-local → ISO ; omis si vide
      closesAt: closesAt.value ? new Date(closesAt.value).toISOString() : undefined,
    }
    pollSchema.parse(await apiFetch<unknown>('/api/polls', { method: 'POST', body: payload }))
    question.value = ''
    options.value = ['', '']
    closesAt.value = ''
    emit('created')
  } catch {
    error.value = 'La création du sondage a échoué.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form
    class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
    data-testid="poll-composer"
    @submit.prevent="submit"
  >
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold text-gray-900">Nouveau sondage</h2>
      <span class="rounded bg-indigo-50 px-2 py-0.5 text-xs text-indigo-700">{{ audienceLabel }}</span>
    </div>

    <input
      v-model="question"
      type="text"
      required
      maxlength="300"
      placeholder="Question"
      class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
    >

    <div class="flex flex-col gap-2">
      <div v-for="(_, index) in options" :key="index" class="flex gap-2">
        <input
          v-model="options[index]"
          type="text"
          maxlength="200"
          :placeholder="`Option ${index + 1}`"
          class="min-w-0 flex-1 rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
        <button
          v-if="options.length > 2"
          type="button"
          class="shrink-0 rounded-md border border-gray-300 px-2 text-gray-400 hover:text-red-600"
          :aria-label="`Retirer l'option ${index + 1}`"
          @click="removeOption(index)"
        >
          ✕
        </button>
      </div>
      <button
        v-if="options.length < MAX_OPTIONS"
        type="button"
        class="self-start text-sm text-indigo-600 hover:underline"
        @click="addOption"
      >
        Ajouter une option
      </button>
    </div>

    <label class="flex flex-col gap-1 text-sm text-gray-600">
      Clôture (optionnel)
      <input
        v-model="closesAt"
        type="datetime-local"
        class="rounded-md border border-gray-300 px-3 py-2 text-gray-700 focus:border-indigo-500 focus:outline-none"
      >
    </label>

    <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

    <button
      type="submit"
      :disabled="submitting || !canSubmit"
      class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
    >
      {{ submitting ? 'Création…' : 'Créer le sondage' }}
    </button>
  </form>
</template>
