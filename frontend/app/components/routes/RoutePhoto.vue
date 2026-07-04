<script setup lang="ts">
import type { Hold, Route } from '~/schemas/routes'

/**
 * Photo d'une voie avec les prises rendues en overlay SVG (coordonnées relatives 0..1) —
 * l'image d'origine n'est jamais modifiée. En mode édition (créateur/admin) : appuyer sur la
 * photo ajoute une prise, appuyer sur une prise la retire.
 */
const props = defineProps<{ route: Route; canEdit?: boolean }>()

const emit = defineEmits<{ saveHolds: [routeId: string, holds: Hold[]] }>()

const imageEl = ref<HTMLImageElement | null>(null)
const showHolds = ref(true)
const editing = ref(false)
const draft = ref<Hold[]>([])

const displayedHolds = computed(() => (editing.value ? draft.value : props.route.holds))
const hasHolds = computed(() => displayedHolds.value.length > 0)

function startEditing() {
  draft.value = [...props.route.holds]
  showHolds.value = true
  editing.value = true
}

function cancelEditing() {
  editing.value = false
}

function saveEditing() {
  emit('saveHolds', props.route.id, draft.value)
  editing.value = false
}

function onPhotoClick(event: MouseEvent) {
  if (!editing.value || !imageEl.value) return
  const rect = imageEl.value.getBoundingClientRect()
  if (rect.width === 0 || rect.height === 0) return
  const x = (event.clientX - rect.left) / rect.width
  const y = (event.clientY - rect.top) / rect.height
  if (x < 0 || x > 1 || y < 0 || y > 1) return
  draft.value = [...draft.value, { x: Math.round(x * 1000) / 1000, y: Math.round(y * 1000) / 1000 }]
}

function removeHold(index: number) {
  if (!editing.value) return
  draft.value = draft.value.filter((_, i) => i !== index)
}
</script>

<template>
  <div v-if="route.photoUrl" class="flex flex-col gap-2">
    <figure class="relative" :class="editing ? 'cursor-crosshair' : ''" @click="onPhotoClick">
      <img
        ref="imageEl"
        :src="route.photoUrl"
        :alt="`Photo de ${route.name}`"
        class="w-full rounded-md object-contain"
        loading="lazy"
      >
      <svg
        v-if="showHolds && hasHolds"
        class="pointer-events-none absolute inset-0 h-full w-full"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        data-testid="holds-overlay"
      >
        <g v-for="(hold, index) in displayedHolds" :key="index">
          <circle
            :cx="hold.x * 100"
            :cy="hold.y * 100"
            r="3"
            fill="none"
            stroke="#f43f5e"
            stroke-width="1"
            vector-effect="non-scaling-stroke"
          />
          <!-- Zone de tap invisible plus large pour retirer une prise en mode édition -->
          <circle
            v-if="editing"
            :cx="hold.x * 100"
            :cy="hold.y * 100"
            r="5"
            fill="transparent"
            class="pointer-events-auto cursor-pointer"
            @click.stop="removeHold(index)"
          />
        </g>
      </svg>
      <button
        v-if="hasHolds && !editing"
        type="button"
        class="absolute right-2 bottom-2 rounded-md bg-white/90 px-2 py-1 text-xs text-gray-700 shadow"
        @click.stop="showHolds = !showHolds"
      >
        {{ showHolds ? 'Masquer les prises' : 'Voir les prises' }}
      </button>
    </figure>

    <div v-if="canEdit" class="flex items-center gap-2">
      <template v-if="!editing">
        <button
          type="button"
          class="text-sm text-indigo-600 hover:underline"
          data-testid="edit-holds"
          @click="startEditing"
        >
          Éditer les prises…
        </button>
      </template>
      <template v-else>
        <p class="grow text-xs text-gray-500">
          Appuyez sur la photo pour ajouter une prise, sur une prise pour la retirer.
        </p>
        <button
          type="button"
          class="shrink-0 rounded-md border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-100"
          @click="cancelEditing"
        >
          Annuler
        </button>
        <button
          type="button"
          class="shrink-0 rounded-md bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700"
          data-testid="save-holds"
          @click="saveEditing"
        >
          Enregistrer
        </button>
      </template>
    </div>
  </div>
</template>
