import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import MessagesPage from '~/pages/messages.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const me: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'member@club.fr',
  displayName: 'Member',
  role: 'MEMBER',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

const coachId = '33333333-3333-4333-8333-333333333333'
const convId = '44444444-4444-4444-8444-444444444444'

const conversation = {
  id: convId,
  type: 'DIRECT',
  title: 'Coach',
  otherUserId: coachId,
  lastMessagePreview: 'Salut',
  lastMessageAt: '2026-07-05T10:00:00Z',
  unread: 2,
}

const generalGroup = {
  id: '66666666-6666-4666-8666-666666666666',
  type: 'GENERAL',
  title: 'Tout le club',
  lastMessagePreview: 'Bienvenue',
  lastMessageAt: '2026-07-05T09:00:00Z',
  unread: 0,
}

const message = {
  id: '55555555-5555-4555-8555-555555555555',
  senderId: coachId,
  senderDisplayName: 'Coach',
  body: 'Salut',
  createdAt: '2026-07-05T10:00:00Z',
}

describe('messages page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  beforeEach(() => {
    clearNuxtData('messages')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown, opts?: { method?: string }) => {
      if (path === '/api/conversations' && (!opts || opts.method !== 'POST'))
        return [conversation, generalGroup]
      if (path === '/api/members')
        return [
          { id: me.id, displayName: 'Member', role: 'MEMBER' },
          { id: coachId, displayName: 'Coach', role: 'COACH' },
        ]
      if (path === `/api/conversations/${convId}/messages` && (!opts || opts.method !== 'POST')) return [message]
      if (path === '/api/messaging/settings') return { generalChatUnlimited: true }
      return {}
    })
  })

  it('lists direct and group conversations with an unread badge and no open thread', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(MessagesPage)
    expect(wrapper.find('[data-testid="conversation-list"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="message-thread"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Coach') // fil 1:1
    expect(wrapper.text()).toContain('Tout le club') // groupe général
    expect(wrapper.find('[data-testid="unread-badge"]').text()).toBe('2')
  })

  it('opens a conversation thread and clears its unread badge', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(MessagesPage)
    await wrapper.find('[data-testid="conversation-list"] button').trigger('click')

    expect(apiFetchMock).toHaveBeenCalledWith(`/api/conversations/${convId}/messages`)
    expect(wrapper.find('[data-testid="message-thread"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Salut')
  })

  it('starts a conversation with a selected peer', async () => {
    apiFetchMock.mockImplementation(async (path: unknown, opts?: { method?: string }) => {
      if (path === '/api/conversations' && opts?.method === 'POST') return conversation
      if (path === '/api/conversations') return []
      if (path === '/api/members')
        return [
          { id: me.id, displayName: 'Member', role: 'MEMBER' },
          { id: coachId, displayName: 'Coach', role: 'COACH' },
        ]
      if (path === `/api/conversations/${convId}/messages`) return [message]
      return {}
    })
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(MessagesPage)
    await wrapper.find('[data-testid="new-conversation"] select').setValue(coachId)
    await wrapper.find('[data-testid="new-conversation"]').trigger('submit')

    expect(apiFetchMock).toHaveBeenCalledWith('/api/conversations', {
      method: 'POST',
      body: { userId: coachId },
    })
  })

  it('lets an admin configure the general-group rate limit', async () => {
    const auth = useAuthStore()
    auth.me = { ...me, role: 'OWNER' }
    auth.initialized = true

    wrapper = await mountSuspended(MessagesPage)
    await nextTick()
    const form = wrapper.find('[data-testid="general-limit-settings"]')
    expect(form.exists()).toBe(true)

    await form.find('input[type="checkbox"]').setValue(true)
    await form.trigger('submit')
    expect(apiFetchMock).toHaveBeenCalledWith(
      '/api/messaging/settings',
      expect.objectContaining({ method: 'PATCH' }),
    )
  })
})
