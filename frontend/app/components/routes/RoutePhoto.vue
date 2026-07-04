<script setup lang="ts">
import type { Route } from '~/schemas/routes'

/**
 * Photo d'une voie avec les prises rendues en overlay SVG (coordonnées relatives 0..1) —
 * l'image d'origine n'est jamais modifiée. L'éditeur d'annotations viendra en Phase 7.
 */
const props = defineProps<{ route: Route }>()

const showHolds = ref(true)

const hasHolds = computed(() => props.route.holds.length > 0)
</script>

<template>
  <figure v-if="route.photoUrl" class="relative">
    <img
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
      <circle
        v-for="(hold, index) in route.holds"
        :key="index"
        :cx="hold.x * 100"
        :cy="hold.y * 100"
        r="3"
        fill="none"
        stroke="#f43f5e"
        stroke-width="1"
        vector-effect="non-scaling-stroke"
      />
    </svg>
    <button
      v-if="hasHolds"
      type="button"
      class="absolute right-2 bottom-2 rounded-md bg-white/90 px-2 py-1 text-xs text-gray-700 shadow"
      @click="showHolds = !showHolds"
    >
      {{ showHolds ? 'Masquer les prises' : 'Voir les prises' }}
    </button>
  </figure>
</template>
