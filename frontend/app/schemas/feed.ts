import { z } from 'zod'
import type { components } from '~/types/api'

export const postAudienceSchema = z.enum(['ORG', 'COACH_STUDENTS'])

export const feedPostSchema = z.object({
  id: z.uuid(),
  audience: postAudienceSchema,
  title: z.string(),
  body: z.string().optional(),
  imageUrls: z.array(z.string()),
  important: z.boolean(),
  pinnedUntil: z.string().optional(),
  authorId: z.uuid(),
  authorDisplayName: z.string(),
  authorRole: z.enum(['OWNER', 'ADMIN', 'COACH', 'MEMBER']),
  createdAt: z.string(),
}) satisfies z.ZodType<components['schemas']['FeedPostResponse']>

export const feedPageSchema = z.object({
  items: z.array(feedPostSchema),
  page: z.number(),
  size: z.number(),
  hasNext: z.boolean(),
}) satisfies z.ZodType<components['schemas']['FeedPageResponse']>

export type PostAudience = z.infer<typeof postAudienceSchema>
export type FeedPost = z.infer<typeof feedPostSchema>
export type FeedPage = z.infer<typeof feedPageSchema>
