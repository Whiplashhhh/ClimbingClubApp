import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import DefaultLayout from '~/layouts/default.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const me: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'prez@club.fr',
  displayName: 'Willem Vanb',
  role: 'OWNER',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

describe('default layout', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    apiFetchMock.mockReset()
    apiFetchMock.mockResolvedValue({ items: [], page: 0, size: 1, hasNext: false, unreadCount: 2 })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('opens the account menu from the avatar and logs out from there', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(DefaultLayout, { slots: { default: () => 'contenu' } })

    // Avatar avec initiales, menu fermé par défaut (la déconnexion n'est plus dans la barre)
    const avatar = wrapper.find('[data-testid="avatar-button"]')
    expect(avatar.text()).toBe('WV')
    expect(wrapper.find('[data-testid="account-menu"]').exists()).toBe(false)

    await avatar.trigger('click')
    const menu = wrapper.find('[data-testid="account-menu"]')
    expect(menu.exists()).toBe(true)
    expect(menu.text()).toContain('Willem Vanb')
    expect(menu.text()).toContain('prez@club.fr')

    const logout = menu.findAll('button').find((b) => b.text() === 'Se déconnecter')
    expect(logout).toBeDefined()
    await logout!.trigger('click')
    expect(apiFetchMock).toHaveBeenCalledWith('/api/auth/logout', { method: 'POST' })
  })

  it('shows the unread badge on the bell', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(DefaultLayout, { slots: { default: () => 'contenu' } })
    await nextTick()
    expect(wrapper.find('[data-testid="bell-badge"]').text()).toBe('2')
  })
})
