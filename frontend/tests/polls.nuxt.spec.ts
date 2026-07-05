import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import PollsPage from '~/pages/polls.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const admin: Me = {
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

const member: Me = { ...admin, id: '33333333-3333-4333-8333-333333333333', displayName: 'Grimpeur', role: 'MEMBER' }

const optA = '44444444-4444-4444-8444-444444444444'
const optB = '55555555-5555-4555-8555-555555555555'

function poll(overrides: Record<string, unknown> = {}) {
  return {
    id: '66666666-6666-4666-8666-666666666666',
    authorId: admin.id,
    authorDisplayName: 'Owner',
    audience: 'ORG',
    question: 'Sortie où ?',
    closed: false,
    createdAt: '2026-07-05T10:00:00Z',
    options: [
      { id: optA, label: 'Fontainebleau', votes: 0 },
      { id: optB, label: 'Le Saussois', votes: 0 },
    ],
    totalVotes: 0,
    ...overrides,
  }
}

describe('polls page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  beforeEach(() => {
    clearNuxtData('polls')
    apiFetchMock.mockReset()
  })

  it('shows the composer for admins and lists polls with results', async () => {
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/polls') return [poll({ totalVotes: 3, options: [
        { id: optA, label: 'Fontainebleau', votes: 2 },
        { id: optB, label: 'Le Saussois', votes: 1 },
      ], myOptionId: optA })]
      return {}
    })
    const auth = useAuthStore()
    auth.me = admin
    auth.initialized = true

    wrapper = await mountSuspended(PollsPage)
    expect(wrapper.find('[data-testid="poll-composer"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Sortie où ?')
    expect(wrapper.text()).toContain('Fontainebleau')
    expect(wrapper.text()).toContain('3 votes')
  })

  it('hides the composer for plain members but lets them vote', async () => {
    apiFetchMock.mockImplementation(async (path: unknown, opts?: { method?: string }) => {
      if (path === '/api/polls' && (!opts || opts.method !== 'POST')) return [poll()]
      if (path === `/api/polls/${poll().id}/vote`)
        return poll({ myOptionId: optA, totalVotes: 1, options: [
          { id: optA, label: 'Fontainebleau', votes: 1 },
          { id: optB, label: 'Le Saussois', votes: 0 },
        ] })
      return {}
    })
    const auth = useAuthStore()
    auth.me = member
    auth.initialized = true

    wrapper = await mountSuspended(PollsPage)
    expect(wrapper.find('[data-testid="poll-composer"]').exists()).toBe(false)

    // Voter pour la première option
    const firstOption = wrapper.find('[data-testid="poll-card"] ul button')
    await firstOption.trigger('click')
    expect(apiFetchMock).toHaveBeenCalledWith(`/api/polls/${poll().id}/vote`, {
      method: 'POST',
      body: { optionId: optA },
    })
  })

  it('disables voting on a closed poll', async () => {
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/polls') return [poll({ closed: true, closesAt: '2026-07-01T10:00:00Z' })]
      return {}
    })
    const auth = useAuthStore()
    auth.me = member
    auth.initialized = true

    wrapper = await mountSuspended(PollsPage)
    const optionButtons = wrapper.findAll('[data-testid="poll-card"] ul button')
    expect(optionButtons.every((b) => b.attributes('disabled') !== undefined)).toBe(true)
    expect(wrapper.text()).toContain('Clôturé le')
  })
})
