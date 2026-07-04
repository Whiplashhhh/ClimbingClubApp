<script setup lang="ts">
import { formatStartTime, type DayOfWeek, type Slot, type SlotChange } from '~/schemas/slots'

const props = defineProps<{ slots: Slot[] }>()

interface Occurrence {
  slot: Slot
  change?: SlotChange
}

interface AgendaDay {
  iso: string
  label: string
  occurrences: Occurrence[]
}

// Index JS de Date#getDay() → jour de la spec
const JS_DAYS: DayOfWeek[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
]

function toIso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

function dayLabel(date: Date, offset: number): string {
  if (offset === 0) return 'Aujourd’hui'
  if (offset === 1) return 'Demain'
  const label = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date)
  return label.charAt(0).toUpperCase() + label.slice(1)
}

/** Les 7 prochains jours avec les séances réelles (annulations/décalages appliqués). */
const days = computed<AgendaDay[]>(() => {
  const today = new Date()
  return Array.from({ length: 7 }, (_, offset) => {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() + offset)
    const iso = toIso(date)
    const weekday = JS_DAYS[date.getDay()]!
    const occurrences = props.slots
      .filter((slot) => slot.dayOfWeek === weekday)
      .map((slot) => ({ slot, change: slot.changes.find((c) => c.date === iso) }))
      .sort((a, b) => a.slot.startTime.localeCompare(b.slot.startTime))
    return { iso, label: dayLabel(date, offset), occurrences }
  }).filter((day) => day.occurrences.length > 0)
})
</script>

<template>
  <section
    class="flex flex-col gap-3 rounded-lg border border-gray-200 bg-white p-4"
    data-testid="week-agenda"
  >
    <h2 class="text-lg font-semibold text-gray-900">Cette semaine</h2>
    <p v-if="days.length === 0" class="text-sm text-gray-500">
      Aucune séance dans les 7 prochains jours.
    </p>
    <div v-for="day in days" :key="day.iso" class="flex flex-col gap-1">
      <h3 class="text-sm font-medium text-gray-500">{{ day.label }}</h3>
      <ul class="flex flex-col gap-1">
        <li
          v-for="occurrence in day.occurrences"
          :key="occurrence.slot.id"
          class="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm"
          :class="occurrence.change?.action === 'CANCELLED' ? 'bg-red-50' : 'bg-gray-50'"
        >
          <span
            class="font-medium tabular-nums"
            :class="occurrence.change ? 'text-gray-400 line-through' : 'text-gray-900'"
          >
            {{ formatStartTime(occurrence.slot.startTime) }}
          </span>
          <span
            v-if="occurrence.change?.action === 'MOVED'"
            class="font-medium text-amber-700 tabular-nums"
          >
            → {{ formatStartTime(occurrence.change.newStartTime ?? '') }}
          </span>
          <span :class="occurrence.change?.action === 'CANCELLED' ? 'text-red-700 line-through' : 'text-gray-800'">
            {{ occurrence.slot.name }}
          </span>
          <span class="text-xs text-gray-500">· {{ occurrence.slot.coachDisplayName }}</span>
          <span
            v-if="occurrence.change?.action === 'CANCELLED'"
            class="ml-auto rounded bg-red-100 px-1.5 py-0.5 text-xs font-medium text-red-700"
          >
            Annulée
          </span>
        </li>
      </ul>
    </div>
  </section>
</template>
