const PUBLIC_PATHS = ['/login', '/register']

export default defineNuxtRouteMiddleware(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized) {
    await auth.fetchMe()
  }
  const isPublic = PUBLIC_PATHS.includes(to.path)
  if (!auth.me && !isPublic) {
    return navigateTo('/login')
  }
  if (auth.me && isPublic) {
    return navigateTo('/')
  }
})
