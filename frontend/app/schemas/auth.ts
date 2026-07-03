import { z } from 'zod'
import type { components } from '~/types/api'

// Schémas Zod des payloads critiques : la clause `satisfies` les verrouille sur le contrat
// OpenAPI généré (toute divergence = erreur de compilation). Les types consommés par l'app sont
// inférés des schémas (non-optionnels car validés à l'exécution).
export const meSchema = z.object({
  id: z.uuid(),
  email: z.string(),
  displayName: z.string(),
  role: z.enum(['OWNER', 'ADMIN', 'COACH', 'MEMBER']),
  status: z.enum(['PENDING', 'ACTIVE', 'DISABLED']),
  organization: z.object({
    id: z.uuid(),
    name: z.string(),
    slug: z.string(),
    climbingType: z.enum(['BOULDER', 'ROPES', 'BOTH']),
  }),
}) satisfies z.ZodType<components['schemas']['MeResponse']>

export const memberSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
  role: z.enum(['OWNER', 'ADMIN', 'COACH', 'MEMBER']),
}) satisfies z.ZodType<components['schemas']['MemberResponse']>

export const pendingMemberSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
  email: z.string(),
}) satisfies z.ZodType<components['schemas']['PendingMemberResponse']>

export type Me = z.infer<typeof meSchema>
export type Member = z.infer<typeof memberSchema>
export type PendingMember = z.infer<typeof pendingMemberSchema>
