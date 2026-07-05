import { z } from 'zod'
import type { components } from '~/types/api'
import { postAudienceSchema, type PostAudience } from '~/schemas/feed'

export const pollOptionSchema = z.object({
  id: z.uuid(),
  label: z.string(),
  votes: z.number(),
}) satisfies z.ZodType<components['schemas']['PollOptionResponse']>

export const pollSchema = z.object({
  id: z.uuid(),
  authorId: z.uuid(),
  authorDisplayName: z.string(),
  audience: postAudienceSchema,
  question: z.string(),
  closesAt: z.string().optional(),
  closed: z.boolean(),
  createdAt: z.string(),
  options: z.array(pollOptionSchema),
  myOptionId: z.uuid().optional(),
  totalVotes: z.number(),
}) satisfies z.ZodType<components['schemas']['PollResponse']>

export type PollOption = z.infer<typeof pollOptionSchema>
export type Poll = z.infer<typeof pollSchema>

export interface CreatePollPayload {
  audience: PostAudience
  question: string
  options: string[]
  closesAt?: string
}

/** Part (0..100) d'une option dans le total, pour la barre de résultats. */
export function votePercent(votes: number, total: number): number {
  return total > 0 ? Math.round((votes / total) * 100) : 0
}
