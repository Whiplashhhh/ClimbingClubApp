<script setup lang="ts">
import { feedPageSchema, type FeedPost } from '~/schemas/feed'

useHead({ title: 'Fil — Belay' })

const PAGE_SIZE = 20

const auth = useAuthStore()

const canPublish = computed(() => auth.isAdmin || auth.me?.role === 'COACH')

const posts = ref<FeedPost[]>([])
const page = ref(0)
const hasNext = ref(false)
const loadError = ref<string | null>(null)

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

async function removePost(id: string) {
  try {
    await apiFetch(`/api/posts/${id}`, { method: 'DELETE' })
    await loadPage(0)
  } catch {
    loadError.value = 'La suppression a échoué.'
  }
}

function canDelete(post: FeedPost): boolean {
  return auth.isAdmin || post.authorId === auth.me?.id
}

await useAsyncData('feed', async () => {
  if (auth.isActive) {
    await loadPage(0)
  }
  return true
})
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-6">
    <section>
      <h1 class="text-2xl font-bold text-gray-900">{{ auth.me.organization.name }}</h1>
      <p class="mt-1 text-sm text-gray-600">
        {{ auth.me.displayName }}
        <span class="ml-1 rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
          code club : {{ auth.me.organization.slug }}
        </span>
      </p>
    </section>

    <section
      v-if="auth.me.status === 'PENDING'"
      class="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-800"
    >
      Votre demande d'adhésion est en attente de validation par un responsable du club.
    </section>

    <template v-else>
      <FeedPostComposer v-if="canPublish" @published="loadPage(0)" />

      <p v-if="loadError" class="text-sm text-red-600">{{ loadError }}</p>

      <section class="flex flex-col gap-3" data-testid="feed">
        <p v-if="posts.length === 0 && !loadError" class="text-sm text-gray-500">
          Aucune publication pour le moment.
        </p>
        <FeedPostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :can-delete="canDelete(post)"
          @delete="removePost"
        />
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
