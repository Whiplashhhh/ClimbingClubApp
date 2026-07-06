export interface PasswordChecks {
  length: boolean
  classes: number
  valid: boolean
}

/**
 * Règles de robustesse alignées sur le back (@StrongPassword) : ≥ 10 caractères et au moins 3 des
 * 4 classes (majuscule, minuscule, chiffre, caractère spécial).
 */
export function passwordChecks(pw: string): PasswordChecks {
  const length = pw.length >= 10
  let classes = 0
  if (/[A-Z]/.test(pw)) classes++
  if (/[a-z]/.test(pw)) classes++
  if (/[0-9]/.test(pw)) classes++
  if (/[^A-Za-z0-9]/.test(pw)) classes++
  return { length, classes, valid: length && classes >= 3 }
}
