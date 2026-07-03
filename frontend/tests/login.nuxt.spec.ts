import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import LoginPage from '~/pages/login.vue'
import RegisterPage from '~/pages/register.vue'

describe('login page', () => {
  it('renders the credentials form', async () => {
    const wrapper = await mountSuspended(LoginPage)
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Connexion')
  })
})

describe('register page', () => {
  it('switches between create and join modes', async () => {
    const wrapper = await mountSuspended(RegisterPage)
    expect(wrapper.text()).toContain('Nom du club')

    const joinTab = wrapper.findAll('button').find((b) => b.text() === 'Rejoindre un club')
    await joinTab!.trigger('click')
    expect(wrapper.text()).toContain('Code du club')
    expect(wrapper.text()).not.toContain('Nom du club')
  })
})
