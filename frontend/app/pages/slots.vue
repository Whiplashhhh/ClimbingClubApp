<script setup lang="ts">
import { z } from 'zod'
import { memberSchema, type Member } from '~/schemas/auth'
import {
  dayLabels,
  slotSchema,
  type CreateSlotChangePayload,
  type DayOfWeek,
  type Slot,
} from '~/schemas/slots'

useHead({ title: 'Créneaux — Belay' })

const auth = useAuthStore()

const canCreate = computed(() => auth.isAdmin || auth.me?.role === 'COACH')

const slots = ref<Slot[]>([])
const clubMembers = ref<Member[]>([])
const error = ref<string | null>(null)

// Formulaire de création
const name = ref('')
const dayOfWeek = ref<DayOfWeek>('MONDAY')
const startTime = ref('18:00')
const durationMinutes = ref(90)
const coachId = ref('')
const submitting = ref(false)

// Un admin choisit le moniteur ; un moniteur crée pour lui-même
const eligibleCoaches = computed(() => clubMembers.value.filter((m) => m.role !== 'MEMBER'))

function canManage(slot: Slot): boolean {
  return auth.isAdmin || slot.coachId === auth.me?.id
}

async function loadSlots() {
  slots.value = z.array(slotSchema).parse(await apiFetch<unknown>('/api/slots'))
}

async function loadMembers() {
  clubMembers.value = z.array(memberSchema).parse(await apiFetch<unknown>('/api/members'))
}

async function createSlot() {
  error.value = null
  submitting.value = true
  try {
    await apiFetch('/api/slots', {
      method: 'POST',
      body: {
        name: name.value,
        dayOfWeek: dayOfWeek.value,
        startTime: startTime.value,
        durationMinutes: durationMinutes.value,
        coachId: auth.isAdmin && coachId.value ? coachId.value : undefined,
      },
    })
    name.value = ''
    await loadSlots()
  } catch {
    error.value = 'La création du créneau a échoué.'
  } finally {
    submitting.value = false
  }
}

async function addMember(slotId: string, userId: string) {
  await mutate(() => apiFetch(`/api/slots/${slotId}/members`, { method: 'POST', body: { userId } }))
}

async function removeMember(slotId: string, userId: string) {
  await mutate(() => apiFetch(`/api/slots/${slotId}/members/${userId}`, { method: 'DELETE' }))
}

async function deleteSlot(slotId: string) {
  await mutate(() => apiFetch(`/api/slots/${slotId}`, { method: 'DELETE' }))
}

async function createChange(slotId: string, payload: CreateSlotChangePayload) {
  await mutate(
    () => apiFetch(`/api/slots/${slotId}/changes`, { method: 'POST', body: payload }),
    'La modification a échoué — vérifie que la date tombe bien le jour du créneau.',
  )
}

async function removeChange(slotId: string, changeId: string) {
  await mutate(() => apiFetch(`/api/slots/${slotId}/changes/${changeId}`, { method: 'DELETE' }))
}

async function mutate(action: () => Promise<unknown>, failureMessage = "L'opération a échoué.") {
  error.value = null
  try {
    await action()
    await loadSlots()
  } catch {
    error.value = failureMessage
  }
}

// Réhydrate les refs depuis la payload (le handler ne se rejoue pas côté client après SSR)
const { data: initial } = await useAsyncData('slots', async () => {
  if (auth.isActive) {
    await Promise.all([loadSlots(), loadMembers()])
  }
  return { slots: slots.value, clubMembers: clubMembers.value }
})
if (initial.value) {
  slots.value = initial.value.slots
  clubMembers.value = initial.value.clubMembers
}

useAutoRefresh(() => {
  if (auth.isActive) loadSlots()
})
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
      <!-- Vue calendrier de la semaine, pour ceux qui gèrent des cours -->
      <SlotsWeekAgenda v-if="canCreate" :slots="slots" />

      <form
        v-if="canCreate"
        class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
        data-testid="slot-form"
        @submit.prevent="createSlot"
      >
        <h2 class="text-lg font-semibold text-gray-900">Nouveau créneau</h2>
        <input
          v-model="name"
          type="text"
          required
          maxlength="120"
          placeholder="Nom (ex. Ados jeudi)"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
        <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <select
            v-model="dayOfWeek"
            class="rounded-md border border-gray-300 px-2 py-2 text-gray-700"
          >
            <option v-for="(label, value) in dayLabels" :key="value" :value="value">
              {{ label }}
            </option>
          </select>
          <input
            v-model="startTime"
            type="time"
            required
            class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
          >
          <label class="flex items-center gap-1 text-sm text-gray-600">
            <input
              v-model.number="durationMinutes"
              type="number"
              min="15"
              max="600"
              step="15"
              required
              class="w-20 rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
            >
            min
          </label>
          <select
            v-if="auth.isAdmin"
            v-model="coachId"
            class="rounded-md border border-gray-300 px-2 py-2 text-gray-700"
          >
            <option value="">Moniteur : moi</option>
            <option v-for="candidate in eligibleCoaches" :key="candidate.id" :value="candidate.id">
              {{ candidate.displayName }}
            </option>
          </select>
        </div>
        <button
          type="submit"
          :disabled="submitting || !name"
          class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Créer le créneau
        </button>
      </form>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <section class="flex flex-col gap-3" data-testid="slots-list">
        <p v-if="slots.length === 0" class="text-sm text-gray-500">
          Aucun créneau pour le moment.
        </p>
        <SlotsSlotCard
          v-for="slot in slots"
          :key="slot.id"
          :slot-item="slot"
          :can-manage="canManage(slot)"
          :club-members="clubMembers"
          @add-member="addMember"
          @remove-member="removeMember"
          @delete-slot="deleteSlot"
          @create-change="createChange"
          @remove-change="removeChange"
        />
      </section>
    </template>
  </div>
</template>
