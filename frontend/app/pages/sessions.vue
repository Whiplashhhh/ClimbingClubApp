<script setup lang="ts">
import { z } from 'zod'
import {
  sessionSchema,
  type CreateAscentPayload,
  type Session,
  type Visibility,
} from '~/schemas/sessions'
import { sectorSchema, type Route } from '~/schemas/routes'
import { memberSchema, type Member } from '~/schemas/auth'

useHead({ title: 'Séances — Belay' })

const auth = useAuthStore()

type Tab = 'mine' | 'club'
const tab = ref<Tab>('mine')

const mine = ref<Session[]>([])
const club = ref<Session[]>([])
const routes = ref<Route[]>([])
const members = ref<Member[]>([])
const error = ref<string | null>(null)

// Nouvelle séance
const note = ref('')
const visibility = ref<Visibility>('CLUB')
const submitting = ref(false)

const shown = computed(() => (tab.value === 'mine' ? mine.value : club.value))

function canManage(session: Session): boolean {
  return session.userId === auth.me?.id
}

async function loadMine() {
  mine.value = z.array(sessionSchema).parse(await apiFetch<unknown>('/api/sessions/mine'))
}
async function loadClub() {
  club.value = z.array(sessionSchema).parse(await apiFetch<unknown>('/api/sessions/club'))
}
async function loadRoutes() {
  const sectors = z.array(sectorSchema).parse(await apiFetch<unknown>('/api/sectors'))
  routes.value = sectors.flatMap((s) => s.routes)
}
async function loadMembers() {
  members.value = z.array(memberSchema).parse(await apiFetch<unknown>('/api/members'))
}

async function startSession() {
  await mutate(async () => {
    await apiFetch('/api/sessions', {
      method: 'POST',
      body: { note: note.value || undefined, visibility: visibility.value },
    })
    note.value = ''
    tab.value = 'mine'
  })
}

async function addAscent(sessionId: string, payload: CreateAscentPayload) {
  await mutate(() => apiFetch(`/api/sessions/${sessionId}/ascents`, { method: 'POST', body: payload }))
}
async function removeAscent(sessionId: string, ascentId: string) {
  await mutate(() => apiFetch(`/api/sessions/${sessionId}/ascents/${ascentId}`, { method: 'DELETE' }))
}
async function changeVisibility(sessionId: string, value: Visibility) {
  const session = mine.value.find((s) => s.id === sessionId)
  await mutate(() =>
    apiFetch(`/api/sessions/${sessionId}`, {
      method: 'PATCH',
      body: { note: session?.note, visibility: value },
    }),
  )
}
async function deleteSession(sessionId: string) {
  await mutate(() => apiFetch(`/api/sessions/${sessionId}`, { method: 'DELETE' }))
}

async function mutate(action: () => Promise<unknown>) {
  error.value = null
  submitting.value = true
  try {
    await action()
    await Promise.all([loadMine(), loadClub()])
  } catch {
    error.value = "L'opération a échoué."
  } finally {
    submitting.value = false
  }
}

async function refresh() {
  if (auth.isActive) await Promise.all([loadMine(), loadClub(), loadRoutes(), loadMembers()])
}

// Le handler ne se rejoue pas côté client après SSR : on renvoie un instantané et on
// réhydrate les refs depuis la payload pour que la page soit remplie même au rechargement.
const { data: initial } = await useAsyncData('sessions', async () => {
  await refresh()
  return { mine: mine.value, club: club.value, routes: routes.value, members: members.value }
})
if (initial.value) {
  mine.value = initial.value.mine
  club.value = initial.value.club
  routes.value = initial.value.routes
  members.value = initial.value.members
}

useAutoRefresh(refresh)
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
      <form
        class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
        data-testid="session-form"
        @submit.prevent="startSession"
      >
        <h2 class="text-lg font-semibold text-gray-900">Nouvelle séance</h2>
        <input
          v-model="note"
          type="text"
          maxlength="500"
          placeholder="Note (optionnel)"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
        <div class="flex items-center justify-between gap-2">
          <label class="flex items-center gap-2 text-sm text-gray-600">
            Visible par
            <select
              v-model="visibility"
              class="rounded-md border border-gray-300 px-2 py-1.5 text-gray-700"
            >
              <option value="CLUB">Tout le club</option>
              <option value="FRIENDS">Mes amis</option>
              <option value="PRIVATE">Privé</option>
            </select>
          </label>
          <button
            type="submit"
            :disabled="submitting"
            class="rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Lancer la séance
          </button>
        </div>
      </form>

      <div class="flex gap-2 border-b border-gray-200">
        <button
          type="button"
          class="border-b-2 px-3 py-2 text-sm"
          :class="tab === 'mine' ? 'border-indigo-600 font-semibold text-indigo-600' : 'border-transparent text-gray-500'"
          @click="tab = 'mine'"
        >
          Mes séances
        </button>
        <button
          type="button"
          class="border-b-2 px-3 py-2 text-sm"
          :class="tab === 'club' ? 'border-indigo-600 font-semibold text-indigo-600' : 'border-transparent text-gray-500'"
          @click="tab = 'club'"
        >
          Activité du club
        </button>
      </div>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <section class="flex flex-col gap-3" data-testid="sessions-list">
        <p v-if="shown.length === 0" class="text-sm text-gray-500">
          {{ tab === 'mine' ? 'Aucune séance enregistrée.' : 'Aucune activité partagée.' }}
        </p>
        <SessionsSessionCard
          v-for="session in shown"
          :key="session.id"
          :session="session"
          :can-manage="canManage(session)"
          :routes="routes"
          :members="members"
          @add-ascent="addAscent"
          @remove-ascent="removeAscent"
          @change-visibility="changeVisibility"
          @delete-session="deleteSession"
        />
      </section>
    </template>
  </div>
</template>
