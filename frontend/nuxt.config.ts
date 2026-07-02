import tailwindcss from '@tailwindcss/vite'

// Cible du proxy API : le back Spring Boot (même origine côté navigateur,
// indispensable pour le cookie de session et le CSRF).
const apiProxyTarget = process.env.NUXT_API_PROXY_TARGET ?? 'http://localhost:8080'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  modules: ['@nuxt/eslint', '@pinia/nuxt', '@vite-pwa/nuxt'],

  css: ['~/assets/css/main.css'],

  vite: {
    plugins: [tailwindcss()],
  },

  typescript: {
    strict: true,
  },

  routeRules: {
    '/api/**': { proxy: `${apiProxyTarget}/api/**` },
  },

  pwa: {
    registerType: 'autoUpdate',
    manifest: {
      name: 'Belay',
      short_name: 'Belay',
      description: "Gestion de club d'escalade",
      lang: 'fr',
      display: 'standalone',
      start_url: '/',
      theme_color: '#4f46e5',
      background_color: '#ffffff',
      icons: [
        { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
        { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
        {
          src: '/icons/icon-512.png',
          sizes: '512x512',
          type: 'image/png',
          purpose: 'maskable',
        },
      ],
    },
    workbox: {
      navigateFallback: '/',
      // Ne jamais servir les appels API depuis le cache du service worker
      navigateFallbackDenylist: [/^\/api\//],
      globPatterns: ['**/*.{js,css,html,png,svg,ico}'],
    },
  },
})
