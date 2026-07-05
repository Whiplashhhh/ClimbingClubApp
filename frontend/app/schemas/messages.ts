import { z } from 'zod'
import type { components } from '~/types/api'

export const conversationSchema = z.object({
  id: z.uuid(),
  otherUserId: z.uuid(),
  otherDisplayName: z.string(),
  lastMessagePreview: z.string().optional(),
  lastMessageAt: z.string(),
  unread: z.number(),
}) satisfies z.ZodType<components['schemas']['ConversationResponse']>

export const messageSchema = z.object({
  id: z.uuid(),
  senderId: z.uuid(),
  senderDisplayName: z.string(),
  body: z.string(),
  createdAt: z.string(),
}) satisfies z.ZodType<components['schemas']['MessageResponse']>

export type Conversation = z.infer<typeof conversationSchema>
export type Message = z.infer<typeof messageSchema>
