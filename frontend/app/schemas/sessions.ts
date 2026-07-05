import { z } from 'zod'
import type { components } from '~/types/api'

export const visibilitySchema = z.enum(['CLUB', 'FRIENDS', 'PRIVATE'])

export const ascentSchema = z.object({
  id: z.uuid(),
  routeId: z.uuid(),
  routeName: z.string(),
  routeGrade: z.string(),
  rating: z.number().optional(),
  topHold: z.number().optional(),
  durationSeconds: z.number().optional(),
  belayerName: z.string().optional(),
}) satisfies z.ZodType<components['schemas']['AscentResponse']>

export const sessionSchema = z.object({
  id: z.uuid(),
  userId: z.uuid(),
  userDisplayName: z.string(),
  startedAt: z.string(),
  note: z.string().optional(),
  visibility: visibilitySchema,
  ascents: z.array(ascentSchema),
}) satisfies z.ZodType<components['schemas']['SessionResponse']>

export type Visibility = z.infer<typeof visibilitySchema>
export type Ascent = z.infer<typeof ascentSchema>
export type Session = z.infer<typeof sessionSchema>

export interface CreateAscentPayload {
  routeId: string
  rating?: number
  topHold?: number
  durationSeconds?: number
  belayerUserId?: string
  belayerName?: string
}

export const visibilityLabels: Record<Visibility, string> = {
  CLUB: 'Tout le club',
  FRIENDS: 'Mes amis',
  PRIVATE: 'Privé',
}

export function formatDuration(seconds: number | undefined): string | null {
  if (seconds === undefined) return null
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m} min${s ? ` ${s}s` : ''}` : `${s}s`
}
