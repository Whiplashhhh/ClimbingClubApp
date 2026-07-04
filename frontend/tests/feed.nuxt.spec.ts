import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import IndexPage from '~/pages/index.vue'
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

const feedPage = {
  items: [
    {
      id: '55555555-5555-4555-8555-555555555555',
      audience: 'COACH_STUDENTS',
      title: 'Séance « Ados perf » du 10/07/2026 annulée',
      body: 'Mur en travaux',
      imageUrls: [],
      important: true,
      pinnedUntil: '2099-01-01T00:00:00Z',
      authorId: ownerMe.id,
      authorDisplayName: 'Coach',
      authorRole: 'COACH',
      createdAt: '2026-07-03T09:00:00Z',
    },
    {
      id: '33333333-3333-4333-8333-333333333333',
      audience: 'ORG',
      title: 'Assemblée générale',
      body: 'Rendez-vous samedi.',
      imageUrls: [],
      important: false,
      authorId: ownerMe.id,
      authorDisplayName: 'Owner',
      authorRole: 'OWNER',
      createdAt: '2026-07-03T10:00:00Z',
    },
  ],
  page: 0,
  size: 20,
  hasNext: false,
}

describe('feed page', () => {
  // La clé useAsyncData('feed') est partagée : démonter entre les tests évite qu'une
  // instance précédente capte le handler de la suivante.
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('feed')
    apiFetchMock.mockReset()
    apiFetchMock.mockResolvedValue(feedPage)
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('shows the composer to admins and renders the feed', async () => {
    const auth = useAuthStore()
    auth.me = ownerMe
    auth.initialized = true

    wrapper = await mountSuspended(IndexPage)
    expect(wrapper.find('[data-testid="post-composer"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Assemblée générale')
    expect(apiFetchMock).toHaveBeenCalledWith('/api/feed?page=0&size=20')

    // Le post important (annulation) est mis en évidence et marqué épinglé
    expect(wrapper.text()).toContain('Important')
    expect(wrapper.find('[data-testid="pinned-chip"]').exists()).toBe(true)
  })

  it('hides the composer from plain members but shows the feed', async () => {
    const auth = useAuthStore()
    auth.me = { ...ownerMe, role: 'MEMBER' }
    auth.initialized = true

    wrapper = await mountSuspended(IndexPage)
    expect(wrapper.find('[data-testid="post-composer"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Assemblée générale')
  })

  it('shows a join-requests banner to admins when requests are pending', async () => {
    apiFetchMock.mockImplementation(async (path: unknown) => {
      if (path === '/api/members/pending')
        return [
          {
            id: '44444444-4444-4444-8444-444444444444',
            displayName: 'Grimpeur',
            email: 'grimpeur@club.fr',
          },
        ]
      return feedPage
    })
    const auth = useAuthStore()
    auth.me = ownerMe
    auth.initialized = true

    wrapper = await mountSuspended(IndexPage)
    expect(wrapper.find('[data-testid="pending-banner"]').exists()).toBe(true)
    expect(wrapper.text()).toContain("demande d'adhésion en attente")
  })

  it('shows the pending banner instead of the feed for pending members', async () => {
    const auth = useAuthStore()
    auth.me = { ...ownerMe, role: 'MEMBER', status: 'PENDING' }
    auth.initialized = true

    wrapper = await mountSuspended(IndexPage)
    expect(wrapper.text()).toContain('en attente de validation')
    expect(apiFetchMock).not.toHaveBeenCalled()
  })
})
