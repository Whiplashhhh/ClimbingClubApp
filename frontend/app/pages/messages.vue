<script setup lang="ts">
import { z } from 'zod'
import { conversationSchema, messageSchema, type Conversation, type Message } from '~/schemas/messages'
import { memberSchema, type Member } from '~/schemas/auth'

useHead({ title: 'Messages — Belay' })

const auth = useAuthStore()

const conversations = ref<Conversation[]>([])
const members = ref<Member[]>([])
const selectedId = ref<string | null>(null)
const thread = ref<Message[]>([])
const draft = ref('')
const newPeerId = ref('')
const error = ref<string | null>(null)
const sending = ref(false)

const selected = computed(() => conversations.value.find((c) => c.id === selectedId.value) ?? null)
// Membres à qui l'on peut tenter d'écrire : tout le monde sauf soi (le serveur valide l'éligibilité)
const peerCandidates = computed(() => members.value.filter((m) => m.id !== auth.me?.id))

async function loadConversations() {
  conversations.value = z.array(conversationSchema).parse(await apiFetch<unknown>('/api/conversations'))
}
async function loadMembers() {
  members.value = z.array(memberSchema).parse(await apiFetch<unknown>('/api/members'))
}

async function openConversation(id: string) {
  error.value = null
  selectedId.value = id
  try {
    thread.value = z
      .array(messageSchema)
      .parse(await apiFetch<unknown>(`/api/conversations/${id}/messages`))
    // Ouvrir marque comme lu côté serveur : on remet le badge local à zéro
    const conv = conversations.value.find((c) => c.id === id)
    if (conv) conv.unread = 0
  } catch {
    error.value = 'La conversation n’a pas pu être ouverte.'
  }
}

function closeThread() {
  selectedId.value = null
  thread.value = []
}

async function send() {
  if (!draft.value.trim() || !selectedId.value) return
  error.value = null
  sending.value = true
  try {
    const message = messageSchema.parse(
      await apiFetch<unknown>(`/api/conversations/${selectedId.value}/messages`, {
        method: 'POST',
        body: { body: draft.value.trim() },
      }),
    )
    thread.value = [...thread.value, message]
    draft.value = ''
    await loadConversations()
  } catch {
    error.value = 'Le message n’a pas pu être envoyé.'
  } finally {
    sending.value = false
  }
}

async function startConversation() {
  if (!newPeerId.value) return
  error.value = null
  try {
    const conv = conversationSchema.parse(
      await apiFetch<unknown>('/api/conversations', {
        method: 'POST',
        body: { userId: newPeerId.value },
      }),
    )
    newPeerId.value = ''
    await loadConversations()
    await openConversation(conv.id)
  } catch {
    error.value = "Impossible d'ouvrir ce fil : une relation moniteur ↔ élève est requise."
  }
}

async function refresh() {
  if (auth.isActive) await Promise.all([loadConversations(), loadMembers()])
}

// Réhydrate depuis la payload : le handler useAsyncData ne se rejoue pas côté client après SSR.
const { data: initial } = await useAsyncData('messages', async () => {
  await refresh()
  return { conversations: conversations.value, members: members.value }
})
if (initial.value) {
  conversations.value = initial.value.conversations
  members.value = initial.value.members
}

useAutoRefresh(async () => {
  if (!auth.isActive) return
  await loadConversations()
  // Rafraîchit le fil ouvert pour voir les nouveaux messages
  if (selectedId.value) await openConversation(selectedId.value)
})

function timeLabel(iso: string): string {
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso))
}
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

      <!-- Vue liste : conversations + démarrage d'un nouveau fil -->
      <template v-if="!selected">
        <form
          class="flex gap-2 rounded-lg border border-gray-200 bg-white p-4"
          data-testid="new-conversation"
          @submit.prevent="startConversation"
        >
          <select
            v-model="newPeerId"
            class="min-w-0 flex-1 rounded-md border border-gray-300 px-2 py-2 text-gray-700"
            aria-label="Membre à contacter"
          >
            <option value="" disabled>Nouveau message à…</option>
            <option v-for="m in peerCandidates" :key="m.id" :value="m.id">{{ m.displayName }}</option>
          </select>
          <button
            type="submit"
            :disabled="!newPeerId"
            class="shrink-0 rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Écrire
          </button>
        </form>

        <section class="flex flex-col gap-2" data-testid="conversation-list">
          <p v-if="conversations.length === 0" class="text-sm text-gray-500">Aucune conversation.</p>
          <button
            v-for="conv in conversations"
            :key="conv.id"
            type="button"
            class="flex items-center justify-between gap-2 rounded-lg border border-gray-200 bg-white p-3 text-left hover:border-indigo-300"
            @click="openConversation(conv.id)"
          >
            <div class="min-w-0">
              <p class="flex items-center gap-1.5 font-medium text-gray-900">
                <span
                  v-if="conv.type !== 'DIRECT'"
                  class="shrink-0 rounded bg-indigo-50 px-1.5 py-0.5 text-[10px] font-semibold text-indigo-700"
                >
                  {{ conv.type === 'GENERAL' ? 'Club' : 'Groupe' }}
                </span>
                <span class="truncate">{{ conv.title }}</span>
              </p>
              <p v-if="conv.lastMessagePreview" class="truncate text-sm text-gray-500">
                {{ conv.lastMessagePreview }}
              </p>
            </div>
            <span
              v-if="conv.unread > 0"
              class="shrink-0 rounded-full bg-indigo-600 px-2 py-0.5 text-xs font-semibold text-white"
              data-testid="unread-badge"
            >
              {{ conv.unread }}
            </span>
          </button>
        </section>
      </template>

      <!-- Vue fil : messages + zone d'envoi -->
      <template v-else>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="rounded-md border border-gray-300 px-2 py-1 text-sm text-gray-600 hover:bg-gray-100"
            @click="closeThread"
          >
            ← Retour
          </button>
          <h2 class="text-lg font-semibold text-gray-900">{{ selected.title }}</h2>
        </div>

        <section class="flex flex-col gap-2" data-testid="message-thread">
          <p v-if="thread.length === 0" class="text-sm text-gray-500">Aucun message. Écrivez le premier !</p>
          <div
            v-for="message in thread"
            :key="message.id"
            class="max-w-[80%] rounded-lg px-3 py-2 text-sm"
            :class="
              message.senderId === auth.me.id
                ? 'self-end bg-indigo-600 text-white'
                : 'self-start bg-gray-100 text-gray-800'
            "
          >
            <p
              v-if="selected.type !== 'DIRECT' && message.senderId !== auth.me.id"
              class="mb-0.5 text-[10px] font-semibold text-indigo-600"
            >
              {{ message.senderDisplayName }}
            </p>
            <p class="whitespace-pre-line">{{ message.body }}</p>
            <p
              class="mt-1 text-[10px]"
              :class="message.senderId === auth.me.id ? 'text-indigo-200' : 'text-gray-400'"
            >
              {{ timeLabel(message.createdAt) }}
            </p>
          </div>
        </section>

        <form class="flex gap-2" @submit.prevent="send">
          <input
            v-model="draft"
            type="text"
            maxlength="4000"
            placeholder="Votre message…"
            class="min-w-0 flex-1 rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          >
          <button
            type="submit"
            :disabled="sending || !draft.trim()"
            class="shrink-0 rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            Envoyer
          </button>
        </form>
      </template>
    </template>
  </div>
</template>
