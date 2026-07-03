package app.belay;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Client de test représentant un utilisateur avec son propre cookie jar (session + CSRF), comme
 * un navigateur : lit le token XSRF via GET /api/auth/csrf et le renvoie en header X-XSRF-TOKEN.
 */
public class ApiActor {

    private final TestRestTemplate rest = new TestRestTemplate(HttpClientOption.ENABLE_COOKIES);
    private final String baseUrl;

    public ApiActor(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public <T> ResponseEntity<T> get(String path, Class<T> type) {
        return rest.getForEntity(baseUrl + path, type);
    }

    public <T> ResponseEntity<T> post(String path, Object body, Class<T> type) {
        return exchange(HttpMethod.POST, path, body, type, csrfToken());
    }

    public <T> ResponseEntity<T> postWithoutCsrf(String path, Object body, Class<T> type) {
        return exchange(HttpMethod.POST, path, body, type, null);
    }

    public <T> ResponseEntity<T> patch(String path, Object body, Class<T> type) {
        return exchange(HttpMethod.PATCH, path, body, type, csrfToken());
    }

    private <T> ResponseEntity<T> exchange(
            HttpMethod method, String path, Object body, Class<T> type, String csrfToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (csrfToken != null) {
            headers.set("X-XSRF-TOKEN", csrfToken);
        }
        return rest.exchange(baseUrl + path, method, new HttpEntity<>(body, headers), type);
    }

    private String csrfCookie;

    /**
     * Convention SPA : le header X-XSRF-TOKEN doit porter la valeur BRUTE du cookie XSRF-TOKEN
     * (pas le token masqué XOR rendu par le serveur). On capture le Set-Cookie initial.
     */
    private String csrfToken() {
        var response = rest.getForEntity(baseUrl + "/api/auth/csrf", String.class);
        for (String setCookie : response.getHeaders().getOrDefault(HttpHeaders.SET_COOKIE, java.util.List.of())) {
            if (setCookie.startsWith("XSRF-TOKEN=")) {
                int end = setCookie.indexOf(';');
                csrfCookie = setCookie.substring("XSRF-TOKEN=".length(), end > 0 ? end : setCookie.length());
            }
        }
        return csrfCookie;
    }
}
