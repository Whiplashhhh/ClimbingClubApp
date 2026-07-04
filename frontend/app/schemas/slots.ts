import { z } from 'zod'
import type { components } from '~/types/api'

export const dayOfWeekSchema = z.enum([
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
])

export const slotMemberSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
}) satisfies z.ZodType<components['schemas']['SlotMemberResponse']>

export const slotChangeSchema = z.object({
  id: z.uuid(),
  date: z.string(),
  action: z.enum(['CANCELLED', 'MOVED']),
  newStartTime: z.string().optional(),
  note: z.string().optional(),
}) satisfies z.ZodType<components['schemas']['SlotChangeResponse']>

export const slotSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  dayOfWeek: dayOfWeekSchema,
  startTime: z.string(),
  durationMinutes: z.number(),
  coachId: z.uuid(),
  coachDisplayName: z.string(),
  members: z.array(slotMemberSchema),
  changes: z.array(slotChangeSchema),
}) satisfies z.ZodType<components['schemas']['SlotResponse']>

export type DayOfWeek = z.infer<typeof dayOfWeekSchema>
export type SlotMember = z.infer<typeof slotMemberSchema>
export type SlotChange = z.infer<typeof slotChangeSchema>
export type Slot = z.infer<typeof slotSchema>

export interface CreateSlotChangePayload {
  date: string
  action: SlotChange['action']
  newStartTime?: string
  note?: string
}

export const dayLabels: Record<DayOfWeek, string> = {
  MONDAY: 'Lundi',
  TUESDAY: 'Mardi',
  WEDNESDAY: 'Mercredi',
  THURSDAY: 'Jeudi',
  FRIDAY: 'Vendredi',
  SATURDAY: 'Samedi',
  SUNDAY: 'Dimanche',
}

/** "18:00:00" → "18:00" (le back sérialise en ISO, secondes omises si nulles) */
export function formatStartTime(time: string): string {
  return time.slice(0, 5)
}
