<script setup lang="ts">
import { z } from 'zod'
import { pendingMemberSchema } from '~/schemas/auth'
import { feedPageSchema, type FeedPost } from '~/schemas/feed'
import { pollSchema, type Poll } from '~/schemas/polls'

useHead({ title: 'Fil — Belay' })

const PAGE_SIZE = 20

const auth = useAuthStore()

const canPublish = computed(() => auth.isStaff)
// Composeur : publier une info (texte + images) OU un sondage
const composerMode = ref<'info' | 'poll'>('info')

const posts = ref<FeedPost[]>([])
const polls = ref<Poll[]>([])
const page = ref(0)
const hasNext = ref(false)
const loadError = ref<string | null>(null)

// Fil unifié : posts et sondages entrelacés, plus récents d'abord
type FeedItem = { kind: 'post'; post: FeedPost; at: string } | { kind: 'poll'; poll: Poll; at: string }
const timeline = computed<FeedItem[]>(() => {
  const items: FeedItem[] = [
    ...posts.value.map((post) => ({ kind: 'post' as const, post, at: post.createdAt })),
    ...polls.value.map((poll) => ({ kind: 'poll' as const, poll, at: poll.createdAt })),
  ]
  return items.sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
})

async function loadPage(target: number) {
  loadError.value = null
  try {
    const data = feedPageSchema.parse(
      await apiFetch<unknown>(`/api/feed?page=${target}&size=${PAGE_SIZE}`),
    )
    posts.value = target === 0 ? data.items : [...posts.value, ...data.items]
    page.value = data.page
    hasNext.value = data.hasNext
  } catch {
    loadError.value = 'Le fil n’a pas pu être chargé.'
  }
}

async function loadPolls() {
  polls.value = z.array(pollSchema).parse(await apiFetch<unknown>('/api/polls'))
}

async function removePost(id: string) {
  try {
    await apiFetch(`/api/posts/${id}`, { method: 'DELETE' })
    await loadPage(0)
  } catch {
    loadError.value = 'La suppression a échoué.'
  }
}

function canDeletePost(post: FeedPost): boolean {
  return auth.isAdmin || post.authorId === auth.me?.id
}
function canDeletePoll(poll: Poll): boolean {
  return auth.isAdmin || poll.authorId === auth.me?.id
}

async function votePoll(pollId: string, optionId: string) {
  loadError.value = null
  try {
    const updated = pollSchema.parse(
      await apiFetch<unknown>(`/api/polls/${pollId}/vote`, { method: 'POST', body: { optionId } }),
    )
    polls.value = polls.value.map((p) => (p.id === updated.id ? updated : p))
  } catch {
    loadError.value = 'Le vote a échoué (sondage clôturé ?).'
    await loadPolls()
  }
}
async function removePoll(pollId: string) {
  try {
    await apiFetch(`/api/polls/${pollId}`, { method: 'DELETE' })
    polls.value = polls.value.filter((p) => p.id !== pollId)
  } catch {
    loadError.value = 'La suppression a échoué.'
  }
}

// Rappel non bloquant pour les admins : demandes d'adhésion à traiter sur /members
const pendingCount = ref(0)

async function refreshPendingCount() {
  if (!auth.isAdmin) return
  try {
    const pending = z
      .array(pendingMemberSchema)
      .parse(await apiFetch<unknown>('/api/members/pending'))
    pendingCount.value = pending.length
  } catch {
    pendingCount.value = 0
  }
}

async function refresh() {
  if (auth.isActive) {
    await Promise.all([loadPage(0), loadPolls(), refreshPendingCount()])
  }
}

// Réhydrate les refs depuis la payload : le handler ne se rejoue pas côté client après SSR,
// sinon le fil serait vide au rechargement / à l'ouverture à froid de la PWA.
const { data: initial } = await useAsyncData('feed', async () => {
  await refresh()
  return {
    posts: posts.value,
    polls: polls.value,
    page: page.value,
    hasNext: hasNext.value,
    pendingCount: pendingCount.value,
  }
})
if (initial.value) {
  posts.value = initial.value.posts
  polls.value = initial.value.polls
  page.value = initial.value.page
  hasNext.value = initial.value.hasNext
  pendingCount.value = initial.value.pendingCount
}

useAutoRefresh(refresh)
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-6">
    <section class="flex items-start justify-between gap-2">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ auth.me.organization.name }}</h1>
        <p class="mt-1 text-sm text-gray-600">
          {{ auth.me.displayName }}
          <span class="ml-1 rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
            code club : {{ auth.me.organization.slug }}
          </span>
        </p>
      </div>
      <button
        v-if="auth.isActive"
        type="button"
        class="shrink-0 rounded-md border border-gray-300 p-2 text-gray-500 hover:bg-gray-100"
        aria-label="Actualiser le fil"
        data-testid="refresh-button"
        @click="refresh"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          class="h-4 w-4"
        >
          <path d="M21 12a9 9 0 1 1-2.64-6.36L21 8" />
          <path d="M21 3v5h-5" />
        </svg>
      </button>
    </section>

    <section
      v-if="auth.me.status === 'PENDING'"
      class="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-800"
    >
      Votre demande d'adhésion est en attente de validation par un responsable du club.
    </section>

    <template v-else>
      <NuxtLink
        v-if="pendingCount > 0"
        to="/members"
        class="rounded-lg border border-indigo-200 bg-indigo-50 p-3 text-sm text-indigo-800 hover:bg-indigo-100"
        data-testid="pending-banner"
      >
        {{ pendingCount }} demande{{ pendingCount > 1 ? 's' : '' }} d'adhésion en attente —
        gérer dans « Membres »
      </NuxtLink>

      <!-- Composeur : bascule Info / Sondage (encadrants uniquement) -->
      <div v-if="canPublish" class="flex flex-col gap-3">
        <div class="flex gap-2" data-testid="composer-toggle">
          <button
            type="button"
            class="rounded-md border px-3 py-1.5 text-sm"
            :class="composerMode === 'info' ? 'border-indigo-600 bg-indigo-50 font-semibold text-indigo-700' : 'border-gray-300 text-gray-600'"
            @click="composerMode = 'info'"
          >
            Info
          </button>
          <button
            type="button"
            class="rounded-md border px-3 py-1.5 text-sm"
            :class="composerMode === 'poll' ? 'border-indigo-600 bg-indigo-50 font-semibold text-indigo-700' : 'border-gray-300 text-gray-600'"
            @click="composerMode = 'poll'"
          >
            Sondage
          </button>
        </div>
        <FeedPostComposer v-if="composerMode === 'info'" @published="loadPage(0)" />
        <PollsPollComposer v-else @created="loadPolls" />
      </div>

      <p v-if="loadError" class="text-sm text-red-600">{{ loadError }}</p>

      <section class="flex flex-col gap-3" data-testid="feed">
        <p v-if="timeline.length === 0 && !loadError" class="text-sm text-gray-500">
          Aucune publication pour le moment.
        </p>
        <template v-for="item in timeline" :key="item.kind + (item.kind === 'post' ? item.post.id : item.poll.id)">
          <FeedPostCard
            v-if="item.kind === 'post'"
            :post="item.post"
            :can-delete="canDeletePost(item.post)"
            @delete="removePost"
          />
          <PollsPollCard
            v-else
            :poll="item.poll"
            :can-delete="canDeletePoll(item.poll)"
            @vote="votePoll"
            @delete-poll="removePoll"
          />
        </template>
      </section>

      <button
        v-if="hasNext"
        type="button"
        class="self-center rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-100"
        @click="loadPage(page + 1)"
      >
        Voir plus
      </button>
    </template>
  </div>
</template>
