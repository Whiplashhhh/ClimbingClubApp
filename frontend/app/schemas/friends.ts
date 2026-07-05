import { z } from 'zod'
import type { components } from '~/types/api'

// Un ami (ou un demandeur), identité minimale. Les champs sont validés à l'exécution : on les
// rend non-optionnels côté app même si le contrat les marque optionnels (inclusion non-null).
export const friendSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
}) satisfies z.ZodType<components['schemas']['FriendResponse']>

export const friendRequestSchema = z.object({
  requester: friendSchema,
}) satisfies z.ZodType<components['schemas']['FriendRequestResponse']>

export type Friend = z.infer<typeof friendSchema>
export type FriendRequest = z.infer<typeof friendRequestSchema>
