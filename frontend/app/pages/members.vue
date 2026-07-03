<script setup lang="ts">
import { z } from 'zod'
import { memberSchema, pendingMemberSchema, type Member, type PendingMember } from '~/schemas/auth'

useHead({ title: 'Membres — Belay' })

const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  OWNER: 'Président',
  ADMIN: 'Admin',
  COACH: 'Moniteur',
  MEMBER: 'Membre',
}

const members = ref<Member[]>([])
const pendingMembers = ref<PendingMember[]>([])
const actionError = ref<string | null>(null)

async function loadMembers() {
  if (!auth.isActive) return
  members.value = z.array(memberSchema).parse(await apiFetch<unknown>('/api/members'))
  if (auth.isAdmin) {
    pendingMembers.value = z
      .array(pendingMemberSchema)
      .parse(await apiFetch<unknown>('/api/members/pending'))
  }
}

async function approve(id: string) {
  actionError.value = null
  try {
    await apiFetch(`/api/members/${id}/approve`, { method: 'POST' })
    await loadMembers()
  } catch {
    actionError.value = "L'approbation a échoué."
  }
}

await useAsyncData('members', async () => {
  await loadMembers()
  return true
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
      <section v-if="auth.isAdmin && pendingMembers.length > 0">
        <h2 class="mb-2 text-lg font-semibold text-gray-900">Demandes d'adhésion</h2>
        <p v-if="actionError" class="mb-2 text-sm text-red-600">{{ actionError }}</p>
        <ul class="flex flex-col gap-2">
          <li
            v-for="pending in pendingMembers"
            :key="pending.id"
            class="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-3"
          >
            <span class="text-sm text-gray-800">
              {{ pending.displayName }}
              <span class="text-gray-500">({{ pending.email }})</span>
            </span>
            <button
              class="rounded-md bg-indigo-600 px-3 py-1 text-sm text-white hover:bg-indigo-700"
              @click="approve(pending.id)"
            >
              Approuver
            </button>
          </li>
        </ul>
      </section>

      <section>
        <h2 class="mb-2 text-lg font-semibold text-gray-900">Membres</h2>
        <ul class="flex flex-col gap-2">
          <li
            v-for="member in members"
            :key="member.id"
            class="flex items-center justify-between rounded-lg border border-gray-200 bg-white p-3"
          >
            <span class="text-sm text-gray-800">{{ member.displayName }}</span>
            <span class="text-xs text-gray-500">{{ roleLabels[member.role] }}</span>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>
