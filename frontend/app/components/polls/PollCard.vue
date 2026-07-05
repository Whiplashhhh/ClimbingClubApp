<script setup lang="ts">
import { votePercent, type Poll } from '~/schemas/polls'

const props = defineProps<{
  poll: Poll
  canDelete: boolean
}>()

const emit = defineEmits<{
  vote: [pollId: string, optionId: string]
  deletePoll: [pollId: string]
}>()

const closesLabel = computed(() => {
  if (!props.poll.closesAt) return null
  const date = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(props.poll.closesAt),
  )
  return props.poll.closed ? `Clôturé le ${date}` : `Clôture le ${date}`
})
</script>

<template>
  <article class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4" data-testid="poll-card">
    <div class="flex items-start justify-between gap-2">
      <div class="min-w-0">
        <p class="font-semibold text-gray-900">{{ poll.question }}</p>
        <p class="text-xs text-gray-500">
          {{ poll.authorDisplayName }}
          <span v-if="poll.audience === 'COACH_STUDENTS'" class="ml-1 text-indigo-600">· vos élèves</span>
        </p>
      </div>
      <button
        v-if="canDelete"
        type="button"
        class="shrink-0 text-xs text-gray-400 hover:text-red-600"
        @click="emit('deletePoll', poll.id)"
      >
        Supprimer
      </button>
    </div>

    <ul class="flex flex-col gap-2">
      <li v-for="option in poll.options" :key="option.id">
        <button
          type="button"
          :disabled="poll.closed"
          class="group relative block w-full overflow-hidden rounded-md border px-3 py-2 text-left text-sm disabled:cursor-not-allowed"
          :class="
            option.id === poll.myOptionId
              ? 'border-indigo-500 bg-indigo-50'
              : 'border-gray-200 hover:border-indigo-300'
          "
          :aria-pressed="option.id === poll.myOptionId"
          @click="emit('vote', poll.id, option.id)"
        >
          <!-- Barre de résultats en fond, proportionnelle aux voix -->
          <span
            class="absolute inset-y-0 left-0 bg-indigo-100/70"
            :style="{ width: votePercent(option.votes, poll.totalVotes) + '%' }"
            aria-hidden="true"
          />
          <span class="relative flex items-center justify-between gap-2">
            <span class="min-w-0 truncate text-gray-800">
              <span v-if="option.id === poll.myOptionId" class="mr-1 text-indigo-600">✓</span>
              {{ option.label }}
            </span>
            <span class="shrink-0 text-xs text-gray-500">
              {{ option.votes }} · {{ votePercent(option.votes, poll.totalVotes) }}%
            </span>
          </span>
        </button>
      </li>
    </ul>

    <div class="flex items-center justify-between text-xs text-gray-500">
      <span>{{ poll.totalVotes }} vote{{ poll.totalVotes > 1 ? 's' : '' }}</span>
      <span v-if="closesLabel" :class="poll.closed ? 'text-red-500' : ''">{{ closesLabel }}</span>
    </div>
  </article>
</template>
