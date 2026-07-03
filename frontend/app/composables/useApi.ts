import type { NitroFetchOptions, NitroFetchRequest } from 'nitropack'

type ApiFetchOptions = NitroFetchOptions<NitroFetchRequest>

function readCsrfCookie(): string | null {
  if (import.meta.client) {
    const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
    return match?.[1] ? decodeURIComponent(match[1]) : null
  }
  return useCookie('XSRF-TOKEN').value ?? null
}

/**
 * Wrapper $fetch pour l'API : ajoute le token CSRF (valeur brute du cookie XSRF-TOKEN → header
 * X-XSRF-TOKEN) sur les mutations, et forwarde le cookie de session pendant le SSR.
 */
export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const method = String(options.method ?? 'GET').toUpperCase()
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string> | undefined),
    ...(import.meta.server ? useRequestHeaders(['cookie']) : {}),
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    let token = readCsrfCookie()
    if (!token) {
      // Amorce le cookie CSRF avant la première mutation
      await $fetch('/api/auth/csrf')
      token = readCsrfCookie()
    }
    if (token) headers['X-XSRF-TOKEN'] = token
  }

  return $fetch<T>(path, { ...options, headers }) as Promise<T>
}
