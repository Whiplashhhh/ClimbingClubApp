<script setup lang="ts">
import { z } from 'zod'
import {
  climbTypeLabels,
  sectorSchema,
  type ClimbType,
  type Hold,
  type Route,
  type Sector,
} from '~/schemas/routes'

useHead({ title: 'Voies — Belay' })

const auth = useAuthStore()

const canManageSectors = computed(() => auth.isAdmin)
const canCreateRoutes = computed(() => auth.isAdmin || auth.me?.role === 'COACH')

const sectors = ref<Sector[]>([])
const error = ref<string | null>(null)

// Formulaire secteur (admins)
const sectorName = ref('')
const sectorPhoto = ref<File | null>(null)
const sectorPhotoInput = ref<HTMLInputElement | null>(null)

// Formulaire voie (admins + moniteurs)
const routeName = ref('')
const routeGrade = ref('')
const routeType = ref<ClimbType>('BOULDER')
const routeSectorId = ref('')
const routePhoto = ref<File | null>(null)
const routePhotoInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)

function onSectorPhotoChange(event: Event) {
  sectorPhoto.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

function onRoutePhotoChange(event: Event) {
  routePhoto.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function loadSectors() {
  sectors.value = z.array(sectorSchema).parse(await apiFetch<unknown>('/api/sectors'))
  if (!routeSectorId.value && sectors.value.length > 0) {
    routeSectorId.value = sectors.value[0]!.id
  }
}

async function createSector() {
  await mutate(async () => {
    const form = new FormData()
    form.append('meta', new Blob([JSON.stringify({ name: sectorName.value })], { type: 'application/json' }))
    if (sectorPhoto.value) form.append('photo', sectorPhoto.value)
    await apiFetch('/api/sectors', { method: 'POST', body: form })
    sectorName.value = ''
    sectorPhoto.value = null
    if (sectorPhotoInput.value) sectorPhotoInput.value.value = ''
  })
}

async function createRoute() {
  await mutate(async () => {
    const meta = {
      sectorId: routeSectorId.value,
      name: routeName.value,
      grade: routeGrade.value,
      climbType: routeType.value,
    }
    const form = new FormData()
    form.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))
    if (routePhoto.value) form.append('photo', routePhoto.value)
    await apiFetch('/api/routes', { method: 'POST', body: form })
    routeName.value = ''
    routeGrade.value = ''
    routePhoto.value = null
    if (routePhotoInput.value) routePhotoInput.value.value = ''
  })
}

async function deleteRoute(routeId: string) {
  await mutate(() => apiFetch(`/api/routes/${routeId}`, { method: 'DELETE' }))
}

async function saveHolds(routeId: string, holds: Hold[]) {
  await mutate(() => apiFetch(`/api/routes/${routeId}/holds`, { method: 'PUT', body: { holds } }))
}

async function deleteSector(sectorId: string) {
  await mutate(
    () => apiFetch(`/api/sectors/${sectorId}`, { method: 'DELETE' }),
    'Suppression impossible — le secteur contient encore des voies.',
  )
}

function canManageRoute(route: Route): boolean {
  return auth.isAdmin || route.createdById === auth.me?.id
}

async function mutate(action: () => Promise<unknown>, failureMessage = "L'opération a échoué.") {
  error.value = null
  submitting.value = true
  try {
    await action()
    await loadSectors()
  } catch {
    error.value = failureMessage
  } finally {
    submitting.value = false
  }
}

await useAsyncData('sectors', async () => {
  if (auth.isActive) await loadSectors()
  return true
})

useAutoRefresh(() => {
  if (auth.isActive) loadSectors()
})
</script>

<template>
  <div v-if="auth.me" class="flex flex-col gap-6">
    <section
      v-if="auth.me.status === 'PENDING'"
      class="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-800"
    >
      Votre demande d'adhésion est en attente de validation par un responsable du club.
    </section>

    <template v-else>
      <form
        v-if="canManageSectors"
        class="flex flex-col gap-2 rounded-lg border border-gray-200 bg-white p-4"
        data-testid="sector-form"
        @submit.prevent="createSector"
      >
        <h2 class="text-lg font-semibold text-gray-900">Nouveau secteur</h2>
        <input
          v-model="sectorName"
          type="text"
          required
          maxlength="120"
          placeholder="Nom (ex. Dévers)"
          class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
        >
        <label class="flex flex-col gap-1 text-sm text-gray-600">
          Photo du mur (optionnel)
          <input
            ref="sectorPhotoInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="text-gray-600"
            @change="onSectorPhotoChange"
          >
        </label>
        <button
          type="submit"
          :disabled="submitting || !sectorName"
          class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Créer le secteur
        </button>
      </form>

      <form
        v-if="canCreateRoutes && sectors.length > 0"
        class="flex flex-col gap-2 rounded-lg border border-gray-200 bg-white p-4"
        data-testid="route-form"
        @submit.prevent="createRoute"
      >
        <h2 class="text-lg font-semibold text-gray-900">Nouvelle voie</h2>
        <div class="grid grid-cols-2 gap-2">
          <input
            v-model="routeName"
            type="text"
            required
            maxlength="120"
            placeholder="Nom (ex. La bleue)"
            class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          >
          <input
            v-model="routeGrade"
            type="text"
            required
            maxlength="10"
            placeholder="Cotation (6a+, V5…)"
            class="rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          >
          <select
            v-model="routeType"
            class="rounded-md border border-gray-300 px-2 py-2 text-gray-700"
          >
            <option value="BOULDER">Bloc</option>
            <option value="ROPE">Voie</option>
          </select>
          <select
            v-model="routeSectorId"
            class="rounded-md border border-gray-300 px-2 py-2 text-gray-700"
          >
            <option v-for="sector in sectors" :key="sector.id" :value="sector.id">
              {{ sector.name }}
            </option>
          </select>
        </div>
        <label class="flex flex-col gap-1 text-sm text-gray-600">
          Photo de la voie (optionnel)
          <input
            ref="routePhotoInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="text-gray-600"
            @change="onRoutePhotoChange"
          >
        </label>
        <button
          type="submit"
          :disabled="submitting || !routeName || !routeGrade || !routeSectorId"
          class="self-end rounded-md bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          Créer la voie
        </button>
      </form>

      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>

      <p v-if="sectors.length === 0" class="text-sm text-gray-500">
        Aucun secteur pour le moment{{ canManageSectors ? ' — créez le premier ci-dessus.' : '.' }}
      </p>

      <section
        v-for="sector in sectors"
        :key="sector.id"
        class="flex flex-col gap-3"
        data-testid="sector"
      >
        <div class="flex items-center justify-between gap-2">
          <h2 class="text-lg font-semibold text-gray-900">{{ sector.name }}</h2>
          <button
            v-if="canManageSectors && sector.routes.length === 0"
            type="button"
            class="text-xs text-gray-400 hover:text-red-600"
            @click="deleteSector(sector.id)"
          >
            Supprimer
          </button>
        </div>
        <img
          v-if="sector.photoUrl"
          :src="sector.photoUrl"
          :alt="`Mur du secteur ${sector.name}`"
          class="max-h-64 w-full rounded-md object-cover"
          loading="lazy"
        >
        <p v-if="sector.routes.length === 0" class="text-sm text-gray-500">
          Aucune voie dans ce secteur.
        </p>
        <article
          v-for="route in sector.routes"
          :key="route.id"
          class="flex flex-col gap-2 rounded-lg border border-gray-200 bg-white p-4"
        >
          <div class="flex items-center justify-between gap-2">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-gray-900">{{ route.name }}</span>
              <span class="rounded bg-indigo-50 px-2 py-0.5 text-xs font-medium text-indigo-700">
                {{ route.grade }}
              </span>
              <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
                {{ climbTypeLabels[route.climbType] }}
              </span>
            </div>
            <button
              v-if="canManageRoute(route)"
              type="button"
              class="text-xs text-gray-400 hover:text-red-600"
              @click="deleteRoute(route.id)"
            >
              Supprimer
            </button>
          </div>
          <RoutesRoutePhoto
            :route="route"
            :can-edit="canManageRoute(route)"
            @save-holds="saveHolds"
          />
          <p class="text-xs text-gray-500">Par {{ route.createdByDisplayName }}</p>
        </article>
      </section>
    </template>
  </div>
</template>
