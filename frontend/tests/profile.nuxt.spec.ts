import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import ProfilePage from '~/pages/profile.vue'
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

describe('profile page', () => {
  let wrapper: VueWrapper<unknown> | undefined

  beforeEach(() => {
    apiFetchMock.mockReset()
    apiFetchMock.mockResolvedValue(undefined)
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
  })

  it('prefills the edit form and shows role/club info', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(ProfilePage)
    expect(wrapper.text()).toContain('Membre')
    // Le formulaire d'infos est pré-rempli avec le nom et l'email courants
    const editInputs = wrapper.findAll('[data-testid="edit-info"] input')
    expect((editInputs[0]!.element as HTMLInputElement).value).toBe('Grimpeur')
    expect((editInputs[1]!.element as HTMLInputElement).value).toBe('grimpeur@club.fr')

    const submit = wrapper.find('[data-testid="change-password"] button[type="submit"]')
    expect(submit.attributes('disabled')).toBeDefined()
  })

  it('patches the profile when info is saved with the current password', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(ProfilePage)
    const editInputs = wrapper.findAll('[data-testid="edit-info"] input')
    await editInputs[0]!.setValue('Nouveau Nom')
    await editInputs[2]!.setValue('current-password')
    await wrapper.find('[data-testid="edit-info"]').trigger('submit')

    expect(apiFetchMock).toHaveBeenCalledWith('/api/auth/profile', {
      method: 'PATCH',
      body: { displayName: 'Nouveau Nom', email: 'grimpeur@club.fr', currentPassword: 'current-password' },
    })
  })

  it('submits a password change when the form is valid', async () => {
    const auth = useAuthStore()
    auth.me = me
    auth.initialized = true

    wrapper = await mountSuspended(ProfilePage)
    const inputs = wrapper.findAll('[data-testid="change-password"] input[type="password"]')
    await inputs[0]!.setValue('old-password')
    await inputs[1]!.setValue('new-s3cure-pass')
    await inputs[2]!.setValue('new-s3cure-pass')
    await wrapper.find('[data-testid="change-password"]').trigger('submit')

    expect(apiFetchMock).toHaveBeenCalledWith('/api/auth/change-password', {
      method: 'POST',
      body: { currentPassword: 'old-password', newPassword: 'new-s3cure-pass' },
    })
    expect(wrapper.text()).toContain('Mot de passe mis à jour')
  })
})
