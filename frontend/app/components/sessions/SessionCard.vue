<script setup lang="ts">
import {
  formatDuration,
  visibilityLabels,
  type CreateAscentPayload,
  type Session,
  type Visibility,
} from '~/schemas/sessions'
import type { Route } from '~/schemas/routes'
import type { Member } from '~/schemas/auth'

const props = defineProps<{
  session: Session
  canManage: boolean
  /** Voies du club, pour le sélecteur d'ascension (propriétaire uniquement) */
  routes: Route[]
  /** Membres du club, pour choisir l'assureur (propriétaire uniquement) */
  members: Member[]
}>()

const emit = defineEmits<{
  addAscent: [sessionId: string, payload: CreateAscentPayload]
  removeAscent: [sessionId: string, ascentId: string]
  changeVisibility: [sessionId: string, visibility: Visibility]
  deleteSession: [sessionId: string]
}>()

const adding = ref(false)
const routeId = ref('')
const rating = ref('')
const topHold = ref('')
const durationMin = ref('')
const belayerUserId = ref('')
const belayerName = ref('')

// Le grimpeur ne peut pas s'assurer lui-même : on retire l'auteur de la liste des assureurs.
const belayerCandidates = computed(() =>
  props.members.filter((m) => m.id !== props.session.userId),
)

const dateLabel = computed(() =>
  new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(props.session.startedAt),
  ),
)

function submitAscent() {
  if (!routeId.value) return
  // Un membre choisi prime sur le nom libre (le back rejette le nom libre dans ce cas).
  emit('addAscent', props.session.id, {
    routeId: routeId.value,
    rating: rating.value ? Number(rating.value) : undefined,
    topHold: topHold.value ? Number(topHold.value) : undefined,
    durationSeconds: durationMin.value ? Math.round(Number(durationMin.value) * 60) : undefined,
    belayerUserId: belayerUserId.value || undefined,
    belayerName: belayerUserId.value ? undefined : belayerName.value || undefined,
  })
  routeId.value = ''
  rating.value = ''
  topHold.value = ''
  durationMin.value = ''
  belayerUserId.value = ''
  belayerName.value = ''
  adding.value = false
}
</script>

<template>
  <article class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4">
    <div class="flex items-start justify-between gap-2">
      <div>
        <p class="font-semibold text-gray-900">{{ session.userDisplayName }}</p>
        <p class="text-xs text-gray-500">{{ dateLabel }}</p>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <select
          v-if="canManage"
          :value="session.visibility"
          class="rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-700"
          :aria-label="`Confidentialité de la séance`"
          @change="
            emit(
              'changeVisibility',
              session.id,
              ($event.target as HTMLSelectElement).value as Visibility,
            )
          "
        >
          <option v-for="(label, value) in visibilityLabels" :key="value" :value="value">
            {{ label }}
          </option>
        </select>
        <span v-else class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
          {{ visibilityLabels[session.visibility] }}
        </span>
        <button
          v-if="canManage"
          type="button"
          class="text-xs text-gray-400 hover:text-red-600"
          @click="emit('deleteSession', session.id)"
        >
          Supprimer
        </button>
      </div>
    </div>

    <p v-if="session.note" class="text-sm whitespace-pre-line text-gray-700">{{ session.note }}</p>

    <ul v-if="session.ascents.length > 0" class="flex flex-col gap-1.5">
      <li
        v-for="ascent in session.ascents"
        :key="ascent.id"
        class="flex items-center justify-between gap-2 rounded-md bg-gray-50 px-3 py-2 text-sm"
      >
        <div class="min-w-0">
          <span class="font-medium text-gray-900">{{ ascent.routeName }}</span>
          <span class="ml-1 rounded bg-indigo-50 px-1.5 py-0.5 text-xs text-indigo-700">
            {{ ascent.routeGrade }}
          </span>
          <span v-if="ascent.rating" class="ml-1 text-amber-500">
            {{ '★'.repeat(ascent.rating) }}<span class="text-gray-300">{{
              '★'.repeat(5 - ascent.rating)
            }}</span>
          </span>
          <span class="ml-1 text-xs text-gray-500">
            <template v-if="ascent.topHold !== undefined">· prise {{ ascent.topHold }}</template>
            <template v-if="formatDuration(ascent.durationSeconds)">
              · {{ formatDuration(ascent.durationSeconds) }}</template
            >
            <template v-if="ascent.belayerName">· assuré par {{ ascent.belayerName }}</template>
          </span>
        </div>
        <button
          v-if="canManage"
          type="button"
          class="shrink-0 text-xs text-gray-400 hover:text-red-600"
          :aria-label="`Retirer ${ascent.routeName}`"
          @click="emit('removeAscent', session.id, ascent.id)"
        >
          ✕
        </button>
      </li>
    </ul>
    <p v-else class="text-sm text-gray-500">Aucune ascension.</p>

    <template v-if="canManage">
      <button
        v-if="!adding"
        type="button"
        class="self-start text-sm text-indigo-600 hover:underline"
        data-testid="add-ascent"
        @click="adding = true"
      >
        Ajouter une ascension…
      </button>
      <form
        v-else
        class="flex flex-col gap-2 rounded-md border border-gray-200 bg-gray-50 p-3"
        @submit.prevent="submitAscent"
      >
        <select
          v-model="routeId"
          required
          class="rounded-md border border-gray-300 px-2 py-2 text-gray-700"
        >
          <option value="" disabled>Choisir une voie…</option>
          <option v-for="route in routes" :key="route.id" :value="route.id">
            {{ route.name }} · {{ route.grade }}
          </option>
        </select>
        <div class="grid grid-cols-3 gap-2">
          <label class="flex flex-col gap-1 text-xs text-gray-500">
            Note /5
            <input
              v-model="rating"
              type="number"
              min="1"
              max="5"
              class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
            >
          </label>
          <label class="flex flex-col gap-1 text-xs text-gray-500">
            Prise max
            <input
              v-model="topHold"
              type="number"
              min="0"
              class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
            >
          </label>
          <label class="flex flex-col gap-1 text-xs text-gray-500">
            Durée (min)
            <input
              v-model="durationMin"
              type="number"
              min="0"
              class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
            >
          </label>
        </div>
        <select
          v-model="belayerUserId"
          class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
          aria-label="Assureur (membre)"
        >
          <option value="">Assureur : membre…</option>
          <option v-for="member in belayerCandidates" :key="member.id" :value="member.id">
            {{ member.displayName }}
          </option>
        </select>
        <input
          v-if="!belayerUserId"
          v-model="belayerName"
          type="text"
          maxlength="120"
          placeholder="…ou un nom libre"
          class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
        >
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
            @click="adding = false"
          >
            Annuler
          </button>
          <button
            type="submit"
            :disabled="!routeId"
            class="rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Ajouter
          </button>
        </div>
      </form>
    </template>
  </article>
</template>
