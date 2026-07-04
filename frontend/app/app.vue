<template>
  <div>
    <NuxtRouteAnnouncer />
    <!-- NuxtLayout est indispensable : sans lui, layouts/default.vue (en-tête,
         navigation, déconnexion) n'est jamais rendu -->
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>
