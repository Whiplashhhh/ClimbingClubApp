<script setup lang="ts">
import { dayLabels, formatStartTime, type Slot } from '~/schemas/slots'
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
}>()

const auth = useAuthStore()

const selectedMemberId = ref('')

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
  </article>
</template>
