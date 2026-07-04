import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import NotificationsPage from '~/pages/notifications.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const memberMe: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'membre@club.fr',
  displayName: 'Grimpeur',
  role: 'MEMBER',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

const notificationsPage = {
  items: [
    {
      id: '33333333-3333-4333-8333-333333333333',
      type: 'SLOT_CANCELLED',
      message: 'Séance « Ados jeudi » du 09/07/2026 annulée — Coach malade',
      createdAt: '2026-07-04T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  hasNext: false,
  unreadCount: 1,
}

describe('notifications page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('notifications')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (typeof path === 'string' && path.startsWith('/api/notifications?')) {
        return notificationsPage
      }
      return undefined
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('lists notifications and marks everything read on open', async () => {
    const auth = useAuthStore()
    auth.me = memberMe
    auth.initialized = true

    wrapper = await mountSuspended(NotificationsPage)
    expect(wrapper.text()).toContain('annulée — Coach malade')
    expect(apiFetchMock).toHaveBeenCalledWith('/api/notifications/read-all', { method: 'POST' })

    const notificationsStore = useNotificationsStore()
    expect(notificationsStore.unreadCount).toBe(0)
  })
})
