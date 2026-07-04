<script setup lang="ts">
import type { FeedPost } from '~/schemas/feed'

const props = defineProps<{ post: FeedPost; canDelete: boolean }>()
defineEmits<{ delete: [id: string] }>()

const typeLabels: Record<FeedPost['type'], string> = {
  INFO: 'Info',
  CANCELLATION: 'Cours annulé',
  POSTER: 'Affiche',
}

const typeClasses: Record<FeedPost['type'], string> = {
  INFO: 'bg-indigo-50 text-indigo-700',
  CANCELLATION: 'bg-red-50 text-red-700',
  POSTER: 'bg-emerald-50 text-emerald-700',
}

const createdAtLabel = computed(() =>
  new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(props.post.createdAt),
  ),
)
</script>

<template>
  <!-- Un cours annulé est important : la carte entière est mise en évidence -->
  <article
    class="flex flex-col gap-2 rounded-lg border p-4"
    :class="post.type === 'CANCELLATION' ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white'"
  >
    <div class="flex items-center justify-between gap-2">
      <div class="flex items-center gap-2">
        <span class="rounded px-2 py-0.5 text-xs" :class="typeClasses[post.type]">
          {{ typeLabels[post.type] }}
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
      v-if="post.imageUrl"
      :src="post.imageUrl"
      :alt="post.title"
      class="max-h-96 w-full rounded-md object-contain"
      loading="lazy"
    >

    <p class="text-xs text-gray-500">{{ post.authorDisplayName }} · {{ createdAtLabel }}</p>
  </article>
</template>
