import { z } from 'zod'
import type { components } from '~/types/api'

export const conversationTypeSchema = z.enum(['DIRECT', 'SLOT', 'GENERAL'])

export const conversationSchema = z.object({
  id: z.uuid(),
  type: conversationTypeSchema,
  title: z.string(),
  otherUserId: z.uuid().optional(),
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

export type ConversationType = z.infer<typeof conversationTypeSchema>
export type Conversation = z.infer<typeof conversationSchema>
export type Message = z.infer<typeof messageSchema>
