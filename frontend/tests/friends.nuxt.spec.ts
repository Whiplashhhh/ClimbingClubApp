import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import FriendsPage from '~/pages/friends.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const me: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'grimpeur@club.fr',
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

const friend = { id: '33333333-3333-4333-8333-333333333333', displayName: 'Bob', role: 'MEMBER' }
const requester = { id: '44444444-4444-4444-8444-444444444444', displayName: 'Carol', role: 'MEMBER' }
const stranger = { id: '55555555-5555-4555-8555-555555555555', displayName: 'Dave', role: 'MEMBER' }

describe('friends page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('friends')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/friends') return [{ id: friend.id, displayName: friend.displayName }]
      if (path === '/api/friends/requests/incoming')
        return [{ requester: { id: requester.id, displayName: requester.displayName } }]
      if (path === '/api/members')
        return [{ id: me.id, displayName: 'Grimpeur', role: 'MEMBER' }, friend, requester, stranger]
      return {}
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('lists friends, incoming requests, and only addable members as candidates', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(FriendsPage)
    expect(wrapper.text()).toContain('Bob') // ami
    expect(wrapper.text()).toContain('Carol') // demande reçue

    // Le sélecteur ne propose que Dave : pas moi, pas un ami (Bob), pas un demandeur (Carol)
    const options = wrapper
      .find('[data-testid="friend-request-form"] select')
      .findAll('option')
      .map((o) => o.text())
    expect(options).toContain('Dave')
    expect(options).not.toContain('Bob')
    expect(options).not.toContain('Carol')
    expect(options).not.toContain('Grimpeur')
  })

  it('sends a friend request to the selected member', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(FriendsPage)
    const select = wrapper.find('[data-testid="friend-request-form"] select')
    await select.setValue(stranger.id)
    await wrapper.find('[data-testid="friend-request-form"]').trigger('submit')

    expect(apiFetchMock).toHaveBeenCalledWith('/api/friends/requests', {
      method: 'POST',
      body: { addresseeId: stranger.id },
    })
  })

  it('accepts an incoming request', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(FriendsPage)
    const acceptBtn = wrapper
      .findAll('button')
      .find((b) => b.text() === 'Accepter')
    await acceptBtn!.trigger('click')

    expect(apiFetchMock).toHaveBeenCalledWith(`/api/friends/${requester.id}/accept`, {
      method: 'POST',
    })
  })
})
