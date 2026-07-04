import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import RoutesPage from '~/pages/routes.vue'
import type { Me } from '~/schemas/auth'

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }))
mockNuxtImport('apiFetch', () => apiFetchMock)

const ownerMe: Me = {
  id: '11111111-1111-4111-8111-111111111111',
  email: 'prez@club.fr',
  displayName: 'Prez',
  role: 'OWNER',
  status: 'ACTIVE',
  organization: {
    id: '22222222-2222-4222-8222-222222222222',
    name: 'Mon club',
    slug: 'mon-club',
    climbingType: 'BOTH',
  },
}

const sectors = [
  {
    id: '33333333-3333-4333-8333-333333333333',
    name: 'Dévers',
    photoUrl: 'https://minio.example/mur.png?X-Amz-Signature=abc',
    routes: [
      {
        id: '44444444-4444-4444-8444-444444444444',
        sectorId: '33333333-3333-4333-8333-333333333333',
        name: 'La bleue',
        grade: '6a+',
        climbType: 'BOULDER',
        photoUrl: 'https://minio.example/voie.png?X-Amz-Signature=def',
        holds: [
          { x: 0.25, y: 0.8 },
          { x: 0.5, y: 0.55 },
        ],
        createdById: '55555555-5555-4555-8555-555555555555',
        createdByDisplayName: 'Marie Coach',
        createdAt: '2026-07-04T10:00:00Z',
      },
    ],
  },
]

describe('routes page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    clearNuxtData('sectors')
    apiFetchMock.mockReset()
    apiFetchMock.mockImplementation(async (path: unknown) =>
      path === '/api/sectors' ? sectors : {},
    )
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('shows the wall map with hold overlay and management forms to admins', async () => {
    const auth = useAuthStore()
    auth.me = ownerMe
    auth.initialized = true

    wrapper = await mountSuspended(RoutesPage)
    expect(wrapper.find('[data-testid="sector-form"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="route-form"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Dévers')
    expect(wrapper.text()).toContain('La bleue')
    expect(wrapper.text()).toContain('6a+')
    expect(wrapper.text()).toContain('Bloc')
    expect(wrapper.text()).toContain('Marie Coach')

    // Les prises annotées sont rendues en overlay SVG (2 cercles)
    const overlay = wrapper.find('[data-testid="holds-overlay"]')
    expect(overlay.exists()).toBe(true)
    expect(overlay.findAll('circle')).toHaveLength(2)
  })

  it('is read-only for plain members', async () => {
    const auth = useAuthStore()
    auth.me = { ...ownerMe, role: 'MEMBER' }
    auth.initialized = true

    wrapper = await mountSuspended(RoutesPage)
    expect(wrapper.find('[data-testid="sector-form"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="route-form"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('La bleue')
    expect(wrapper.find('[data-testid="holds-overlay"]').exists()).toBe(true)
  })
})
