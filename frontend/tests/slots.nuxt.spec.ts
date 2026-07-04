import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import SlotsPage from '~/pages/slots.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const coachMe: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'coach@club.fr',
  displayName: 'Coach',
  role: 'COACH',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

const memberId = '33333333-3333-4333-8333-333333333333'

const slots = [
  {
    id: '44444444-4444-4444-8444-444444444444',
    name: 'Ados jeudi',
    dayOfWeek: 'THURSDAY',
    startTime: '18:00',
    durationMinutes: 90,
    coachId: coachMe.id,
    coachDisplayName: 'Coach',
    members: [{ id: memberId, displayName: 'Grimpeur' }],
  },
]

const clubMembers = [
  { id: coachMe.id, displayName: 'Coach', role: 'COACH' },
  { id: memberId, displayName: 'Grimpeur', role: 'MEMBER' },
]

describe('slots page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('slots')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/slots') return slots
      if (path === '/api/members') return clubMembers
      return {}
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('lets a coach create slots and manage their group', async () => {
    const auth = useAuthStore()
    auth.me = coachMe
    auth.initialized = true

    wrapper = await mountSuspended(SlotsPage)
    expect(wrapper.find('[data-testid="slot-form"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ados jeudi')
    expect(wrapper.text()).toContain('Jeudi · 18:00 · 90 min')
    expect(wrapper.text()).toContain('Mon créneau')
    expect(wrapper.text()).toContain('Grimpeur')

    const removeButton = wrapper.find('button[aria-label="Retirer Grimpeur"]')
    expect(removeButton.exists()).toBe(true)
    await removeButton.trigger('click')
    expect(apiFetchMock).toHaveBeenCalledWith(`/api/slots/${slots[0]!.id}/members/${memberId}`, {
      method: 'DELETE',
    })
  })

  it('shows the schedule read-only to plain members', async () => {
    const auth = useAuthStore()
    auth.me = { ...coachMe, id: memberId, displayName: 'Grimpeur', role: 'MEMBER' }
    auth.initialized = true

    wrapper = await mountSuspended(SlotsPage)
    expect(wrapper.find('[data-testid="slot-form"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Ados jeudi')
    // Membre du créneau → badge, mais pas de gestion
    expect(wrapper.text()).toContain('Mon créneau')
    expect(wrapper.find('button[aria-label="Retirer Grimpeur"]').exists()).toBe(false)
  })
})
