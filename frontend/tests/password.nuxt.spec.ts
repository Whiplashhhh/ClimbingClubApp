import { describe, expect, it } from 'vitest'
import { passwordChecks } from '~/utils/password'

describe('passwordChecks', () => {
  it('requires 10+ chars and 3 of 4 classes', () => {
    expect(passwordChecks('short').valid).toBe(false) // trop court
    expect(passwordChecks('onlylowercase').valid).toBe(false) // 1 classe
    expect(passwordChecks('lowercase123').valid).toBe(false) // 2 classes
    expect(passwordChecks('Lowercase123').valid).toBe(true) // 3 classes, 12 chars
    expect(passwordChecks('s3cure-password').valid).toBe(true) // minuscule + chiffre + spécial
  })
})
