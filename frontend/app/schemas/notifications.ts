import { z } from 'zod'
import type { components } from '~/types/api'

export const notificationSchema = z.object({
  id: z.uuid(),
  type: z.enum(['SLOT_CANCELLED', 'SLOT_MOVED']),
  message: z.string(),
  readAt: z.string().optional(),
  createdAt: z.string(),
}) satisfies z.ZodType<components['schemas']['NotificationResponse']>

export const notificationPageSchema = z.object({
  items: z.array(notificationSchema),
  page: z.number(),
  size: z.number(),
  hasNext: z.boolean(),
  unreadCount: z.number(),
}) satisfies z.ZodType<components['schemas']['NotificationPageResponse']>

export type AppNotification = z.infer<typeof notificationSchema>
export type NotificationPage = z.infer<typeof notificationPageSchema>
