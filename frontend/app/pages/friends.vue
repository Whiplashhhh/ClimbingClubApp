<script setup lang="ts">
import { z } from 'zod'
import { friendSchema, friendRequestSchema, type Friend, type FriendRequest } from '~/schemas/friends'
import { memberSchema, type Member } from '~/schemas/auth'

useHead({ title: 'Amis — Belay' })

const auth = useAuthStore()

const friends = ref<Friend[]>([])
const incoming = ref<FriendRequest[]>([])
const members = ref<Member[]>([])
const error = ref<string | null>(null)
const submitting = ref(false)
const selectedMemberId = ref('')

// Membres à qui je peux encore envoyer une demande : ni moi, ni déjà ami, ni déjà demandeur.
const candidates = computed(() => {
  const linked = new Set<string>([auth.me?.id ?? ''])
  friends.value.forEach((f) => linked.add(f.id))
  incoming.value.forEach((r) => linked.add(r.requester.id))
  return members.value.filter((m) => !linked.has(m.id))
})

async function loadFriends() {
  friends.value = z.array(friendSchema).parse(await apiFetch<unknown>('/api/friends'))
}
async function loadIncoming() {
  incoming.value = z
    .array(friendRequestSchema)
    .parse(await apiFetch<unknown>('/api/friends/requests/incoming'))
}
async function loadMembers() {
  members.value = z.array(memberSchema).parse(await apiFetch<unknown>('/api/members'))
}

async function sendRequest() {
  if (!selectedMemberId.value) return
  await mutate(async () => {
    await apiFetch('/api/friends/requests', {
      method: 'POST',
      body: { addresseeId: selectedMemberId.value },
    })
    selectedMemberId.value = ''
  })
}
async function accept(requesterId: string) {
  await mutate(() => apiFetch(`/api/friends/${requesterId}/accept`, { method: 'POST' }))
}
async function reject(requesterId: string) {
  await mutate(() => apiFetch(`/api/friends/${requesterId}`, { method: 'DELETE' }))
}
async function removeFriend(userId: string) {
  await mutate(() => apiFetch(`/api/friends/${userId}`, { method: 'DELETE' }))
}

async function mutate(action: () => Promise<unknown>) {
  error.value = null
  submitting.value = true
  try {
    await action()
    await Promise.all([loadFriends(), loadIncoming(), loadMembers()])
  } catch {
    error.value = "L'opération a échoué."
  } finally {
    submitting.value = false
  }
}

async function refresh() {
  if (auth.isActive) await Promise.all([loadFriends(), loadIncoming(), loadMembers()])
}

// Le handler ne se rejoue pas côté client après SSR : on renvoie un instantané et on réhydrate
// les refs pour que la page soit remplie même au rechargement à froid.
const { data: initial } = await useAsyncData('friends', async () => {
  await refresh()
  return { friends: friends.value, incoming: incoming.value, members: members.value }
})
if (initial.value) {
  friends.value = initial.value.friends
  incoming.value = initial.value.incoming
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
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <form
        class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
        data-testid="friend-request-form"
        @submit.prevent="sendRequest"
      >
        <h2 class="text-lg font-semibold text-gray-900">Ajouter un ami</h2>
        <div class="flex gap-2">
          <select
            v-model="selectedMemberId"
            class="min-w-0 flex-1 rounded-md border border-gray-300 px-2 py-2 text-gray-700"
            aria-label="Membre à qui envoyer une demande"
          >
            <option value="" disabled>Choisir un membre…</option>
            <option v-for="member in candidates" :key="member.id" :value="member.id">
              {{ member.displayName }}
            </option>
          </select>
          <button
            type="submit"
            :disabled="!selectedMemberId || submitting"
            class="shrink-0 rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Envoyer
          </button>
        </div>
        <p v-if="candidates.length === 0" class="text-sm text-gray-500">
          Aucun membre à ajouter pour le moment.
        </p>
      </form>

      <section v-if="incoming.length > 0" data-testid="incoming-requests">
        <h2 class="mb-2 text-lg font-semibold text-gray-900">Demandes reçues</h2>
        <ul class="flex flex-col gap-2">
          <li
            v-for="request in incoming"
            :key="request.requester.id"
            class="flex items-center justify-between gap-2 rounded-lg border border-gray-200 bg-white p-3"
          >
            <span class="min-w-0 truncate text-sm text-gray-800">
              {{ request.requester.displayName }}
            </span>
            <div class="flex shrink-0 gap-2">
              <button
                type="button"
                :disabled="submitting"
                class="rounded-md bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
                @click="accept(request.requester.id)"
              >
                Accepter
              </button>
              <button
                type="button"
                :disabled="submitting"
                class="rounded-md border border-gray-300 px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-50"
                @click="reject(request.requester.id)"
              >
                Refuser
              </button>
            </div>
          </li>
        </ul>
      </section>

      <section data-testid="friends-list">
        <h2 class="mb-2 text-lg font-semibold text-gray-900">Mes amis</h2>
        <p v-if="friends.length === 0" class="text-sm text-gray-500">Aucun ami pour le moment.</p>
        <ul v-else class="flex flex-col gap-2">
          <li
            v-for="friend in friends"
            :key="friend.id"
            class="flex items-center justify-between gap-2 rounded-lg border border-gray-200 bg-white p-3"
          >
            <span class="min-w-0 truncate text-sm text-gray-800">{{ friend.displayName }}</span>
            <button
              type="button"
              :disabled="submitting"
              class="shrink-0 text-xs text-gray-400 hover:text-red-600 disabled:opacity-50"
              @click="removeFriend(friend.id)"
            >
              Retirer
            </button>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>
