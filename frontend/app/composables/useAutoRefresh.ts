/**
 * Rafraîchissement « quasi temps réel » en attendant le push web (Phase 7) :
 * relance `refresh` quand l'onglet redevient visible, puis toutes les `intervalMs`
 * tant qu'il est visible. Ne fait rien côté serveur (SSR).
 */
export function useAutoRefresh(refresh: () => unknown, intervalMs = 60_000): void {
  if (import.meta.server) return

  let timer: number | undefined

  function start() {
    stop()
    timer = window.setInterval(refresh, intervalMs)
  }

  function stop() {
    if (timer !== undefined) {
      window.clearInterval(timer)
      timer = undefined
    }
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'visible') {
      refresh()
      start()
    } else {
      stop()
    }
  }

  onMounted(() => {
    start()
    document.addEventListener('visibilitychange', onVisibilityChange)
  })

  onUnmounted(() => {
    stop()
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })
}
