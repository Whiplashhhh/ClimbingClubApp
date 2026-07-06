<script setup lang="ts">
import { passwordChecks } from '~/utils/password'

const props = defineProps<{ password: string }>()

const checks = computed(() => passwordChecks(props.password))
</script>

<template>
  <ul v-if="password.length > 0" class="flex flex-col gap-0.5 text-xs" data-testid="password-rules">
    <li :class="checks.length ? 'text-green-600' : 'text-gray-500'">
      {{ checks.length ? '✓' : '•' }} au moins 10 caractères
    </li>
    <li :class="checks.classes >= 3 ? 'text-green-600' : 'text-gray-500'">
      {{ checks.classes >= 3 ? '✓' : '•' }} au moins 3 parmi : majuscule, minuscule, chiffre, caractère spécial
    </li>
  </ul>
</template>
