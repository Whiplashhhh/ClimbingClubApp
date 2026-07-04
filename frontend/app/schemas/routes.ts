import { z } from 'zod'
import type { components } from '~/types/api'

export const climbTypeSchema = z.enum(['BOULDER', 'ROPE'])

export const holdSchema = z.object({
  x: z.number(),
  y: z.number(),
}) satisfies z.ZodType<components['schemas']['HoldDto']>

export const routeSchema = z.object({
  id: z.uuid(),
  sectorId: z.uuid(),
  name: z.string(),
  grade: z.string(),
  climbType: climbTypeSchema,
  photoUrl: z.string().optional(),
  holds: z.array(holdSchema),
  createdById: z.uuid(),
  createdByDisplayName: z.string(),
  createdAt: z.string(),
}) satisfies z.ZodType<components['schemas']['RouteResponse']>

export const sectorSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  photoUrl: z.string().optional(),
  routes: z.array(routeSchema),
}) satisfies z.ZodType<components['schemas']['SectorResponse']>

export type ClimbType = z.infer<typeof climbTypeSchema>
export type Hold = z.infer<typeof holdSchema>
export type Route = z.infer<typeof routeSchema>
export type Sector = z.infer<typeof sectorSchema>

export const climbTypeLabels: Record<ClimbType, string> = {
  BOULDER: 'Bloc',
  ROPE: 'Voie',
}
