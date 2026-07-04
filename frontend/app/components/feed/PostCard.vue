<script setup lang="ts">
import type { FeedPost } from '~/schemas/feed'

const props = defineProps<{ post: FeedPost; canDelete: boolean }>()
defineEmits<{ delete: [id: string] }>()

const isPinned = computed(
  () => props.post.pinnedUntil !== undefined && new Date(props.post.pinnedUntil) > new Date(),
)

const createdAtLabel = computed(() =>
  new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(props.post.createdAt),
  ),
)
</script>

<template>
  <!-- Un post important (annulation de séance) est mis en évidence et épinglé en tête du fil -->
  <article
    class="flex flex-col gap-2 rounded-lg border p-4"
    :class="post.important ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white'"
  >
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2">
        <span
          v-if="post.important"
          class="rounded bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700"
        >
          Important
        </span>
        <span
          v-if="isPinned"
          class="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700"
          data-testid="pinned-chip"
        >
          Épinglé
        </span>
        <span
          v-if="post.audience === 'COACH_STUDENTS'"
          class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500"
        >
          Groupe du moniteur
        </span>
      </div>
      <button
        v-if="canDelete"
        type="button"
        class="text-xs text-gray-400 hover:text-red-600"
        @click="$emit('delete', post.id)"
      >
        Supprimer
      </button>
    </div>

    <h3 class="font-semibold text-gray-900">{{ post.title }}</h3>
    <p v-if="post.body" class="text-sm whitespace-pre-line text-gray-700">{{ post.body }}</p>
    <img
      v-for="(imageUrl, index) in post.imageUrls"
      :key="imageUrl"
      :src="imageUrl"
      :alt="`${post.title} — image ${index + 1}`"
      class="max-h-96 w-full rounded-md object-contain"
      loading="lazy"
    >

    <p class="text-xs text-gray-500">{{ post.authorDisplayName }} · {{ createdAtLabel }}</p>
  </article>
</template>
