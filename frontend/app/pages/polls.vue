<script setup lang="ts">
import { z } from 'zod'
import { pollSchema, type Poll } from '~/schemas/polls'

useHead({ title: 'Sondages — Belay' })

const auth = useAuthStore()

const canPublish = computed(() => auth.isAdmin || auth.me?.role === 'COACH')

const polls = ref<Poll[]>([])
const error = ref<string | null>(null)

function canDelete(poll: Poll): boolean {
  return auth.isAdmin || poll.authorId === auth.me?.id
}

async function loadPolls() {
  polls.value = z.array(pollSchema).parse(await apiFetch<unknown>('/api/polls'))
}

async function vote(pollId: string, optionId: string) {
  error.value = null
  try {
    const updated = pollSchema.parse(
      await apiFetch<unknown>(`/api/polls/${pollId}/vote`, {
        method: 'POST',
        body: { optionId },
      }),
    )
    // Remplace le sondage voté en place, sans recharger toute la liste
    polls.value = polls.value.map((p) => (p.id === updated.id ? updated : p))
  } catch {
    error.value = 'Le vote a échoué (sondage clôturé ?).'
    await loadPolls()
  }
}

async function removePoll(pollId: string) {
  error.value = null
  try {
    await apiFetch(`/api/polls/${pollId}`, { method: 'DELETE' })
    polls.value = polls.value.filter((p) => p.id !== pollId)
  } catch {
    error.value = 'La suppression a échoué.'
  }
}

async function refresh() {
  if (auth.isActive) await loadPolls()
}

// Réhydrate depuis la payload : le handler useAsyncData ne se rejoue pas côté client après SSR.
const { data: initial } = await useAsyncData('polls', async () => {
  await refresh()
  return { polls: polls.value }
})
if (initial.value) {
  polls.value = initial.value.polls
}

useAutoRefresh(refresh)
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-6">
    <section
      v-if="auth.me.status === 'PENDING'"
      class="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-800"
    >
      Votre demande d'adhésion est en attente de validation par un responsable du club.
    </section>

    <template v-else>
      <PollsPollComposer v-if="canPublish" @created="loadPolls" />

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <section class="flex flex-col gap-3" data-testid="polls-list">
        <p v-if="polls.length === 0" class="text-sm text-gray-500">Aucun sondage pour le moment.</p>
        <PollsPollCard
          v-for="poll in polls"
          :key="poll.id"
          :poll="poll"
          :can-delete="canDelete(poll)"
          @vote="vote"
          @delete-poll="removePoll"
        />
      </section>
    </template>
  </div>
</template>
