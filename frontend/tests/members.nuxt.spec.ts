import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import MembersPage from '~/pages/members.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const ownerMe: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'owner@club.fr',
  displayName: 'Owner',
  role: 'OWNER',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

const members = [{ id: ownerMe.id, displayName: 'Owner', role: 'OWNER' }]
const pendingMember = {
  id: '44444444-4444-4444-8444-444444444444',
  displayName: 'Grimpeur',
  email: 'grimpeur@club.fr',
}

describe('members page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('members')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/members') return members
      if (path === '/api/members/pending') return [pendingMember]
      return {}
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('lets an admin see and approve a pending request', async () => {
    const auth = useAuthStore()
    auth.me = ownerMe
    auth.initialized = true

    wrapper = await mountSuspended(MembersPage)
    expect(wrapper.text()).toContain('Grimpeur')
    expect(wrapper.text()).toContain('grimpeur@club.fr')

    const approveButton = wrapper.findAll('button').find((b) => b.text() === 'Approuver')
    expect(approveButton).toBeDefined()
    await approveButton!.trigger('click')
    expect(apiFetchMock).toHaveBeenCalledWith(`/api/members/${pendingMember.id}/approve`, {
      method: 'POST',
    })
  })

  it('hides the requests section from plain members', async () => {
    const auth = useAuthStore()
    auth.me = { ...ownerMe, role: 'MEMBER' }
    auth.initialized = true

    wrapper = await mountSuspended(MembersPage)
    expect(wrapper.text()).not.toContain("Demandes d'adhésion")
    expect(wrapper.text()).toContain('Membres')
  })
})
