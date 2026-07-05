import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import SessionsPage from '~/pages/sessions.vue'
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

const mySession = {
  id: '33333333-3333-4333-8333-333333333333',
  userId: me.id,
  userDisplayName: 'Grimpeur',
  startedAt: '2026-07-05T10:00:00Z',
  note: 'Bonne séance',
  visibility: 'CLUB',
  ascents: [
    {
      id: '44444444-4444-4444-8444-444444444444',
      routeId: '55555555-5555-4555-8555-555555555555',
      routeName: 'La bleue',
      routeGrade: '6a+',
      rating: 4,
      topHold: 8,
      durationSeconds: 95,
      belayerName: 'Marie',
    },
  ],
}

const othersSession = {
  ...mySession,
  id: '66666666-6666-4666-8666-666666666666',
  userId: '99999999-9999-4999-8999-999999999999',
  userDisplayName: 'Autre',
  note: 'Séance publique',
  ascents: [],
}

describe('sessions page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('sessions')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/sessions/mine') return [mySession]
      if (path === '/api/sessions/club') return [mySession, othersSession]
      if (path === '/api/sectors')
        return [{ id: 's', name: 'Dévers', routes: mySession.ascents.map((a) => ({
          id: a.routeId, sectorId: 's', name: a.routeName, grade: a.routeGrade,
          climbType: 'BOULDER', holds: [], createdById: me.id, createdByDisplayName: 'x',
          createdAt: '2026-07-05T00:00:00Z',
        })) }]
      return {}
    })
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('shows my sessions with ascents and management controls', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(SessionsPage)
    expect(wrapper.find('[data-testid="session-form"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Bonne séance')
    expect(wrapper.text()).toContain('La bleue')
    expect(wrapper.text()).toContain('assuré par Marie')
    // Ma séance : contrôle d'ajout d'ascension présent
    expect(wrapper.find('[data-testid="add-ascent"]').exists()).toBe(true)
  })

  it('shows club activity read-only for others sessions', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(SessionsPage)
    const clubTab = wrapper.findAll('button').find((b) => b.text() === 'Activité du club')
    await clubTab!.trigger('click')
    expect(wrapper.text()).toContain('Séance publique')
    // La séance d'un autre membre n'a pas de bouton supprimer
    expect(wrapper.text()).toContain('Autre')
    const deleteButtons = wrapper.findAll('button').filter((b) => b.text() === 'Supprimer')
    // Seule ma propre séance (visible dans le fil club) est gérable
    expect(deleteButtons.length).toBe(1)
  })
})
