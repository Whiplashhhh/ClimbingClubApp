import { defineStore } from 'pinia'
import { meSchema, type Me } from '~/schemas/auth'

export interface RegisterPayload {
  email: string
  password: string
  displayName: string
  createOrganization?: { name: string; climbingType: 'BOULDER' | 'ROPES' | 'BOTH' }
  joinSlug?: string
}

export const useAuthStore = defineStore('auth', () => {
  const me = ref<Me | null>(null)
  const initialized = ref(false)

  const isAdmin = computed(() => me.value?.role === 'OWNER' || me.value?.role === 'ADMIN')
  const isActive = computed(() => me.value?.status === 'ACTIVE')

  async function fetchMe(): Promise<void> {
    try {
      const data = await apiFetch<unknown>('/api/auth/me')
      me.value = meSchema.parse(data)
    } catch {
      me.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(email: string, password: string): Promise<void> {
    const data = await apiFetch<unknown>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    })
    me.value = meSchema.parse(data)
    initialized.value = true
  }

  async function register(payload: RegisterPayload): Promise<void> {
    const data = await apiFetch<unknown>('/api/auth/register', {
      method: 'POST',
      body: payload,
    })
    me.value = meSchema.parse(data)
    initialized.value = true
  }

  async function logout(): Promise<void> {
    await apiFetch('/api/auth/logout', { method: 'POST' })
    me.value = null
  }

  return { me, initialized, isAdmin, isActive, fetchMe, login, register, logout }
})
