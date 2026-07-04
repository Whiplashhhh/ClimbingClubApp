<script setup lang="ts">
import {
  dayLabels,
  formatStartTime,
  type CreateSlotChangePayload,
  type Slot,
  type SlotChange,
} from '~/schemas/slots'
import type { Member } from '~/schemas/auth'

const props = defineProps<{
  slotItem: Slot
  canManage: boolean
  /** Membres actifs du club, pour le sélecteur d'ajout (gestionnaires uniquement) */
  clubMembers: Member[]
}>()

const emit = defineEmits<{
  addMember: [slotId: string, userId: string]
  removeMember: [slotId: string, userId: string]
  deleteSlot: [slotId: string]
  createChange: [slotId: string, payload: CreateSlotChangePayload]
  removeChange: [slotId: string, changeId: string]
}>()

const auth = useAuthStore()

const selectedMemberId = ref('')

// Formulaire « annuler / décaler une séance »
const changeFormOpen = ref(false)
const changeDate = ref('')
const changeAction = ref<SlotChange['action']>('CANCELLED')
const changeNewTime = ref('')
const changeNote = ref('')

function changeLabel(change: SlotChange): string {
  const day = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date(`${change.date}T00:00:00`))
  const base =
    change.action === 'CANCELLED'
      ? `Séance du ${day} annulée`
      : `Séance du ${day} décalée à ${formatStartTime(change.newStartTime ?? '')}`
  return change.note ? `${base} — ${change.note}` : base
}

function submitChange() {
  if (!changeDate.value) return
  emit('createChange', props.slotItem.id, {
    date: changeDate.value,
    action: changeAction.value,
    newStartTime: changeAction.value === 'MOVED' ? changeNewTime.value : undefined,
    note: changeNote.value || undefined,
  })
  changeFormOpen.value = false
  changeDate.value = ''
  changeNote.value = ''
}

const isMine = computed(
  () =>
    props.slotItem.coachId === auth.me?.id ||
    props.slotItem.members.some((m) => m.id === auth.me?.id),
)

// Ne proposer que les membres pas encore dans le groupe
const addableMembers = computed(() =>
  props.clubMembers.filter((m) => !props.slotItem.members.some((inSlot) => inSlot.id === m.id)),
)

function onAdd() {
  if (!selectedMemberId.value) return
  emit('addMember', props.slotItem.id, selectedMemberId.value)
  selectedMemberId.value = ''
}
</script>

<template>
  <article class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4">
    <div class="flex items-start justify-between gap-2">
      <div>
        <h3 class="font-semibold text-gray-900">
          {{ slotItem.name }}
          <span
            v-if="isMine"
            class="ml-1 rounded bg-indigo-50 px-2 py-0.5 text-xs font-normal text-indigo-700"
          >
            Mon créneau
          </span>
        </h3>
        <p class="mt-0.5 text-sm text-gray-600">
          {{ dayLabels[slotItem.dayOfWeek] }} · {{ formatStartTime(slotItem.startTime) }} ·
          {{ slotItem.durationMinutes }} min · {{ slotItem.coachDisplayName }}
        </p>
      </div>
      <button
        v-if="canManage"
        type="button"
        class="shrink-0 text-xs text-gray-400 hover:text-red-600"
        @click="emit('deleteSlot', slotItem.id)"
      >
        Supprimer
      </button>
    </div>

    <ul v-if="slotItem.changes.length > 0" class="flex flex-col gap-1">
      <li
        v-for="change in slotItem.changes"
        :key="change.id"
        class="flex items-center justify-between gap-2 rounded-md px-2.5 py-1.5 text-sm"
        :class="change.action === 'CANCELLED' ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-800'"
      >
        <span>{{ changeLabel(change) }}</span>
        <button
          v-if="canManage"
          type="button"
          class="shrink-0 text-xs underline opacity-70 hover:opacity-100"
          @click="emit('removeChange', slotItem.id, change.id)"
        >
          Rétablir
        </button>
      </li>
    </ul>

    <div class="flex flex-wrap items-center gap-1.5">
      <span v-if="slotItem.members.length === 0" class="text-sm text-gray-500">
        Aucun membre dans le groupe.
      </span>
      <span
        v-for="groupMember in slotItem.members"
        :key="groupMember.id"
        class="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2.5 py-0.5 text-xs text-gray-700"
      >
        {{ groupMember.displayName }}
        <button
          v-if="canManage"
          type="button"
          class="text-gray-400 hover:text-red-600"
          :aria-label="`Retirer ${groupMember.displayName}`"
          @click="emit('removeMember', slotItem.id, groupMember.id)"
        >
          ✕
        </button>
      </span>
    </div>

    <div v-if="canManage && addableMembers.length > 0" class="flex items-center gap-2">
      <select
        v-model="selectedMemberId"
        class="grow rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
      >
        <option value="" disabled>Ajouter un membre…</option>
        <option v-for="candidate in addableMembers" :key="candidate.id" :value="candidate.id">
          {{ candidate.displayName }}
        </option>
      </select>
      <button
        type="button"
        :disabled="!selectedMemberId"
        class="shrink-0 rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
        @click="onAdd"
      >
        Ajouter
      </button>
    </div>

    <template v-if="canManage">
      <button
        v-if="!changeFormOpen"
        type="button"
        class="self-start text-sm text-indigo-600 hover:underline"
        @click="changeFormOpen = true"
      >
        Annuler ou décaler une séance…
      </button>
      <form
        v-else
        class="flex flex-col gap-2 rounded-md border border-gray-200 bg-gray-50 p-3"
        data-testid="change-form"
        @submit.prevent="submitChange"
      >
        <div class="flex flex-wrap items-center gap-2">
          <input
            v-model="changeDate"
            type="date"
            required
            class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
          >
          <select
            v-model="changeAction"
            class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
          >
            <option value="CANCELLED">Annulée</option>
            <option value="MOVED">Décalée</option>
          </select>
          <input
            v-if="changeAction === 'MOVED'"
            v-model="changeNewTime"
            type="time"
            required
            class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
          >
        </div>
        <input
          v-model="changeNote"
          type="text"
          maxlength="500"
          placeholder="Motif (optionnel)"
          class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
        >
        <p class="text-xs text-gray-500">
          La date doit tomber un {{ dayLabels[slotItem.dayOfWeek].toLowerCase() }} — le groupe
          sera notifié.
        </p>
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
            @click="changeFormOpen = false"
          >
            Annuler
          </button>
          <button
            type="submit"
            :disabled="!changeDate"
            class="rounded-md bg-indigo-600 px-3 py-1.5 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Confirmer
          </button>
        </div>
      </form>
    </template>
  </article>
</template>
